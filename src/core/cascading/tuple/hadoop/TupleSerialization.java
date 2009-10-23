/*
 * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.tuple.hadoop;

import java.util.HashMap;
import java.util.Map;

import cascading.CascadingException;
import cascading.tuple.IndexTuple;
import cascading.tuple.Tuple;
import cascading.tuple.TupleException;
import cascading.tuple.TuplePair;
import cascading.util.Util;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.apache.hadoop.io.serializer.Serializer;
import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;

/**
 * Class TupleSerialization is an implementation of Hadoop's {@link Serialization} interface.
 * <p/>
 * Typically developers will not use this implementation directly as it is automatically added
 * to any relevant MapReduce jobs via the {@link JobConf}.
 * <p/>
 * By default, all primitive types are natively handled, and {@link org.apache.hadoop.io.BytesWritable}
 * has a pre-configured serialization token since byte arrays are not handled natively by {@link Tuple}.
 */
@SerializationToken(
  tokens = {127},
  classNames = {"org.apache.hadoop.io.BytesWritable"})
public class TupleSerialization extends Configured implements Serialization
  {
  /** Field LOG */
  private static final Logger LOG = Logger.getLogger( TupleSerialization.class );

  /** Field classCache */
  private Map<String, Class> classCache = new HashMap<String, Class>();
  /** Field serializationFactory */
  private SerializationFactory serializationFactory;

  /** Field tokenClassesMap */
  private HashMap<Integer, String> tokenClassesMap;
  /** Field classesTokensMap */
  private HashMap<String, Integer> classesTokensMap;
  /** Field tokenMapSize */
  private long tokensSize = 0;

  /**
   * Adds the given token and className pair as a serialization token property. During object serialization and deserialization,
   * the given token will be used instead of the className when an instance of the className is encountered.
   *
   * @param properties of type Map
   * @param token      of type int
   * @param className  of type String
   */
  public static void addSerializationToken( Map<Object, Object> properties, int token, String className )
    {
    String tokens = getSerializationTokens( properties );

    properties.put( "cascading.serialization.tokens", Util.join( ",", tokens, token + "=" + className ) );
    }

  /**
   * Returns the serialization tokens property.
   *
   * @param properties of type Map
   * @return returns a String
   */
  public static String getSerializationTokens( Map<Object, Object> properties )
    {
    return (String) properties.get( "cascading.serialization.tokens" );
    }

  static String getSerializationTokens( JobConf jobConf )
    {
    return jobConf.get( "cascading.serialization.tokens" );
    }

  /**
   * Adds the given className as a Hadoop IO serialization class.
   *
   * @param properties of type Map
   * @param className  of type String
   */
  public static void addSerialization( Map<Object, Object> properties, String className )
    {
    String serializations = (String) properties.get( "io.serializations" );

    properties.put( "io.serializations", Util.join( ",", serializations, className ) );
    }

  /**
   * Adds this class as a Hadoop Serialization class. This method is safe to call redundantly.
   *
   * @param jobConf of type JobConf
   */
  public static void setSerializations( JobConf jobConf )
    {
    String serializations = getSerializations( jobConf );

    if( serializations.contains( TupleSerialization.class.getName() ) )
      return;

    jobConf.set( "io.serializations", Util.join( ",", serializations, TupleSerialization.class.getName() ) );
    }

  static String getSerializations( JobConf jobConf )
    {
    return jobConf.get( "io.serializations" );
    }

  /** Constructor TupleSerialization creates a new TupleSerialization instance. */
  public TupleSerialization()
    {
    }

  /**
   * Constructor TupleSerialization creates a new TupleSerialization instance.
   *
   * @param conf of type Configuration
   */
  public TupleSerialization( Configuration conf )
    {
    super( conf );
    }

  @Override
  public Configuration getConf()
    {
    if( super.getConf() == null )
      setConf( new JobConf() );

    return super.getConf();
    }

  SerializationFactory getSerializationFactory()
    {
    if( serializationFactory == null )
      serializationFactory = new SerializationFactory( getConf() );

    return serializationFactory;
    }

  /** Must be called before {@link #getClassNameFor(int)} and {@link #getTokenFor(String)} methods. */
  void initTokenMaps()
    {
    if( tokenClassesMap != null )
      return;

    tokenClassesMap = new HashMap<Integer, String>();
    classesTokensMap = new HashMap<String, Integer>();

    String tokenProperty = getSerializationTokens( (JobConf) getConf() );

    if( tokenProperty != null )
      {
      tokenProperty = tokenProperty.replaceAll( "\\s", "" ); // allow for whitespace in token set

      for( String pair : tokenProperty.split( "," ) )
        {
        String[] elements = pair.split( "=" );
        addToken( null, Integer.parseInt( elements[ 0 ] ), elements[ 1 ] );
        }
      }

    String serializationsString = getSerializations( (JobConf) getConf() );

    if( serializationsString == null )
      return;

    String[] serializations = serializationsString.split( "," );

    for( String serializationName : serializations )
      {
      try
        {
        Class type = getConf().getClassByName( serializationName );

        SerializationToken tokenAnnotation = (SerializationToken) type.getAnnotation( SerializationToken.class );

        if( tokenAnnotation == null )
          continue;

        if( tokenAnnotation.tokens().length != tokenAnnotation.classNames().length )
          throw new CascadingException( "serialization annotation tokens and classNames must be the same length" );

        int[] tokens = tokenAnnotation.tokens();

        for( int i = 0; i < tokens.length; i++ )
          addToken( type, tokens[ i ], tokenAnnotation.classNames()[ i ] );
        }
      catch( ClassNotFoundException exception )
        {
        LOG.warn( "unable to load serialization class: " + serializationName, exception );
        }
      }

    tokensSize = tokenClassesMap.size();

    return;
    }

  private void addToken( Class type, int token, String className )
    {
    if( type != null && !type.getName().startsWith( "cascading." ) && token < 128 )
      throw new CascadingException( "serialization annotation tokens may not be less than 128, was: " + token );

    if( tokenClassesMap.containsKey( token ) )
      {
      if( type == null )
        throw new IllegalStateException( "duplicate serialization token: " + token + " for class: " + className + " found in properties" );

      throw new IllegalStateException( "duplicate serialization token: " + token + " for class: " + className + " on serialization: " + type.getName() );
      }

    if( classesTokensMap.containsKey( className ) )
      {
      if( type == null )
        throw new IllegalStateException( "duplicate serialization classname: " + className + " for token: " + token + " found in properties " );

      throw new IllegalStateException( "duplicate serialization classname: " + className + " for token: " + token + " on serialization: " + type.getName() );
      }

    tokenClassesMap.put( token, className );
    classesTokensMap.put( className, token );
    }

  /**
   * Returns the className for the given token.
   *
   * @param token of type int
   * @return a String
   */
  final String getClassNameFor( int token )
    {
    if( tokensSize == 0 )
      return null;

    return tokenClassesMap.get( token );
    }

  /**
   * Returns the token for the given className.
   *
   * @param className of type String
   * @return an Integer
   */
  final Integer getTokenFor( String className )
    {
    if( tokensSize == 0 )
      return null;

    return classesTokensMap.get( className );
    }

  Serializer getNewSerializer( Class type )
    {
    try
      {
      return getSerializationFactory().getSerializer( type );
      }
    catch( NullPointerException exception )
      {
      throw new CascadingException( "unable to load serializer for: " + type.getName() + " from: " + getSerializationFactory().getClass().getName() );
      }
    }

  Deserializer getNewDeserializer( String className )
    {
    try
      {
      return getSerializationFactory().getDeserializer( getClass( className ) );
      }
    catch( NullPointerException exception )
      {
      throw new CascadingException( "unable to load deserializer for: " + className + " from: " + getSerializationFactory().getClass().getName() );
      }
    }

  TuplePairDeserializer getTuplePairDeserializer()
    {
    return new TuplePairDeserializer( getElementReader( true ) );
    }

  /**
   * Method getElementReader returns the elementReader of this TupleSerialization object.
   *
   * @param reuseInstances
   * @return the elementReader (type SerializationElementReader) of this TupleSerialization object.
   */
  public SerializationElementReader getElementReader( boolean reuseInstances )
    {
    return new SerializationElementReader( this );
    }

  TupleDeserializer getTupleDeserializer()
    {
    return new TupleDeserializer( getElementReader( true ) );
    }

  private TuplePairSerializer getTuplePairSerializer()
    {
    return new TuplePairSerializer( getElementWriter() );
    }

  private IndexTupleDeserializer getIndexTupleDeserializer()
    {
    return new IndexTupleDeserializer( getElementReader( false ) );
    }

  /**
   * Method getElementWriter returns the elementWriter of this TupleSerialization object.
   *
   * @return the elementWriter (type SerializationElementWriter) of this TupleSerialization object.
   */
  public SerializationElementWriter getElementWriter()
    {
    return new SerializationElementWriter( this );
    }

  private TupleSerializer getTupleSerializer()
    {
    return new TupleSerializer( getElementWriter() );
    }

  private IndexTupleSerializer getIndexTupleSerializer()
    {
    return new IndexTupleSerializer( getElementWriter() );
    }

  /**
   * Method accept implements {@link Serialization#accept(Class)}.
   *
   * @param c of type Class
   * @return boolean
   */
  public boolean accept( Class c )
    {
    return Tuple.class == c || TuplePair.class == c || IndexTuple.class == c;
    }

  /**
   * Method getDeserializer implements {@link Serialization#getDeserializer(Class)}.
   *
   * @param c of type Class
   * @return Deserializer
   */
  public Deserializer getDeserializer( Class c )
    {
    if( c == Tuple.class )
      return getTupleDeserializer();
    else if( c == TuplePair.class )
      return getTuplePairDeserializer();
    else if( c == IndexTuple.class )
        return getIndexTupleDeserializer();

    throw new IllegalArgumentException( "unknown class, cannot deserialize: " + c.getName() );
    }

  /**
   * Method getSerializer implements {@link Serialization#getSerializer(Class)}.
   *
   * @param c of type Class
   * @return Serializer
   */
  public Serializer getSerializer( Class c )
    {
    if( c == Tuple.class )
      return getTupleSerializer();
    else if( c == TuplePair.class )
      return getTuplePairSerializer();
    else if( c == IndexTuple.class )
        return getIndexTupleSerializer();

    throw new IllegalArgumentException( "unknown class, cannot serialize: " + c.getName() );
    }

  private Class getClass( String className )
    {
    Class type = classCache.get( className );

    if( type != null )
      return type;

    try
      {
      if( className.charAt( 0 ) == '[' )
        type = Class.forName( className, true, Thread.currentThread().getContextClassLoader() );
      else
        type = Thread.currentThread().getContextClassLoader().loadClass( className );
      }
    catch( ClassNotFoundException exception )
      {
      throw new TupleException( "unable to load class named: " + className, exception );
      }

    classCache.put( className, type );

    return type;
    }
  }