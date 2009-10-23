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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cascading.ClusterTestCase;
import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.flow.MultiMapReducePlanner;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.aggregator.Count;
import cascading.operation.regex.RegexParser;
import cascading.operation.regex.RegexSplitter;
import cascading.pipe.CoGroup;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.cogroup.CoGroupClosure;
import cascading.scheme.SequenceFile;
import cascading.scheme.TextLine;
import cascading.tap.Hfs;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntryIterator;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;

public class SerializedPipesTest extends ClusterTestCase
  {
  String inputFileApache = "build/test/data/apache.10.txt";

  String inputFileUpper = "build/test/data/upper.txt";
  String inputFileLower = "build/test/data/lower.txt";

  String outputPath = "build/test/output/tuple/";

  public static class InsertBytes extends BaseOperation implements Function
    {
    String asBytes;

    public InsertBytes( Fields fieldDeclaration, String asBytes )
      {
      super( fieldDeclaration );
      this.asBytes = asBytes;
      }

    public void operate( FlowProcess flowProcess, FunctionCall functionCall )
      {
      functionCall.getOutputCollector().add( new Tuple( new BytesWritable( asBytes.getBytes() ) ) );
      }
    }

  public static class ReplaceAsBytes extends BaseOperation implements Function
    {
    public ReplaceAsBytes( Fields fieldDeclaration )
      {
      super( fieldDeclaration );
      }

    public void operate( FlowProcess flowProcess, FunctionCall functionCall )
      {
      functionCall.getOutputCollector().add( new Tuple( new BytesWritable( functionCall.getArguments().getString( 0 ).getBytes() ) ) );
      }
    }

  public static class InsertRawBytes extends BaseOperation implements Function
    {
    String asBytes;

    public InsertRawBytes( Fields fieldDeclaration, String asBytes )
      {
      super( fieldDeclaration );
      this.asBytes = asBytes;
      }

    public void operate( FlowProcess flowProcess, FunctionCall functionCall )
      {
      functionCall.getOutputCollector().add( new Tuple( (Object) asBytes.getBytes() ) );
      }
    }

  public static class InsertBoolean extends BaseOperation implements Function
    {
    boolean asBoolean;

    public InsertBoolean( Fields fieldDeclaration, boolean asBoolean )
      {
      super( fieldDeclaration );
      this.asBoolean = asBoolean;
      }

    public void operate( FlowProcess flowProcess, FunctionCall functionCall )
      {
      functionCall.getOutputCollector().add( new Tuple( new BooleanWritable( asBoolean ) ) );
      }
    }

  public static class Container implements Serializable, Comparable<String>
    {
    String value;

    public Container( String value )
      {
      this.value = value;
      }

    @Override
    public int compareTo( String o )
      {
      return value.compareTo( o );
      }
    }

  public static class InsertTestText extends BaseOperation implements Function
    {
    private String testText;

    public InsertTestText( Fields fieldDeclaration, String testText )
      {
      super( fieldDeclaration );
      this.testText = testText;
      }

    public void operate( FlowProcess flowProcess, FunctionCall functionCall )
      {
      functionCall.getOutputCollector().add( new Tuple( new TestText( testText ) ) );
      }
    }

  public SerializedPipesTest()
    {
    super( "serialized pipes", true ); // leave cluster testing enabled
    }

  public void testSimpleGroup() throws Exception
    {
    if( !new File( inputFileApache ).exists() )
      fail( "data file not found" );

    copyFromLocal( inputFileApache );

    Tap source = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileApache );

    Pipe pipe = new Pipe( "test" );

    pipe = new Each( pipe, new Fields( "line" ), new RegexParser( new Fields( "ip" ), "^[^ ]*" ), new Fields( "ip" ) );

    pipe = new Each( pipe, new InsertBytes( new Fields( "bytes" ), "inserted text as bytes" ), Fields.ALL );

    pipe = new GroupBy( pipe, new Fields( "ip" ) );

    pipe = new Every( pipe, new Count(), new Fields( "ip", "count" ) );

    pipe = new Each( pipe, new InsertBoolean( new Fields( "boolean" ), false ), Fields.ALL );

    Tap sink = new Hfs( new SequenceFile( Fields.ALL ), outputPath + "/hadoop/serialization", true );

    Map<Object, Object> jobProperties = getProperties();

    TupleSerialization.addSerializationToken( jobProperties, 1000, BooleanWritable.class.getName() );

    Flow flow = new FlowConnector( jobProperties ).connect( source, sink, pipe );

//    flow.writeDOT( "groupcount.dot" );

    flow.complete();

    validateLength( flow.openSource(), 10 ); // validate source, this once, as a sanity check
    validateLength( flow, 8, null );
    }

  public void testCoGroupWritableAsKeyValue() throws Exception
    {
    if( !new File( inputFileLower ).exists() )
      fail( "data file not found" );

    copyFromLocal( inputFileLower );
    copyFromLocal( inputFileUpper );

    Tap sourceLower = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileLower );
    Tap sourceUpper = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileUpper );

    Map sources = new HashMap();

    sources.put( "lower", sourceLower );
    sources.put( "upper", sourceUpper );

    Function splitter = new RegexSplitter( new Fields( "num", "char" ), " " );

    // using null pos so all fields are written
    Tap sink = new Hfs( new SequenceFile( Fields.ALL ), outputPath + "/hadoop/writablekeyvalue", true );

    Pipe pipeLower = new Each( new Pipe( "lower" ), new Fields( "line" ), splitter );
    pipeLower = new Each( pipeLower, new InsertBytes( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeLower = new Each( pipeLower, new InsertBytes( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );

    Pipe pipeUpper = new Each( new Pipe( "upper" ), new Fields( "line" ), splitter );
    pipeUpper = new Each( pipeUpper, new InsertBytes( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeUpper = new Each( pipeUpper, new InsertBytes( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );

    Pipe splice = new CoGroup( pipeLower, new Fields( "group" ), pipeUpper, new Fields( "group" ), Fields.size( 8 ) );

    Flow countFlow = new FlowConnector( getProperties() ).connect( sources, sink, splice );

//    countFlow.writeDOT( "cogroup.dot" );
//    System.out.println( "countFlow =\n" + countFlow );

    countFlow.complete();

    validateLength( countFlow, 25, null );
    }

  public void testCoGroupBytesWritableAsKeyValue() throws Exception
    {
    if( !new File( inputFileLower ).exists() )
      fail( "data file not found" );

    copyFromLocal( inputFileLower );
    copyFromLocal( inputFileUpper );

    Tap sourceLower = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileLower );
    Tap sourceUpper = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileUpper );

    Map sources = new HashMap();

    sources.put( "lower", sourceLower );
    sources.put( "upper", sourceUpper );

    Function splitter = new RegexSplitter( new Fields( "num", "char" ), " " );

    // using null pos so all fields are written
    Tap sink = new Hfs( new TextLine(), outputPath + "/hadoop/byteswritablekeyvalue", true );

    Pipe pipeLower = new Each( new Pipe( "lower" ), new Fields( "line" ), splitter );
    pipeLower = new Each( pipeLower, new Fields( "char" ), new ReplaceAsBytes( new Fields( "char" ) ), Fields.REPLACE );

    Pipe pipeUpper = new Each( new Pipe( "upper" ), new Fields( "line" ), splitter );
    pipeUpper = new Each( pipeUpper, new Fields( "char" ), new ReplaceAsBytes( new Fields( "char" ) ), Fields.REPLACE );

    Pipe splice = new CoGroup( pipeLower, new Fields( "num" ), pipeUpper, new Fields( "num" ), Fields.size( 4 ) );

    Flow countFlow = new FlowConnector( getProperties() ).connect( sources, sink, splice );

//    countFlow.writeDOT( "cogroup.dot" );
//    System.out.println( "countFlow =\n" + countFlow );

    countFlow.complete();

    validateLength( countFlow, 5, null );

    TupleEntryIterator iterator = countFlow.openSink();

    assertEquals( "not equal: tuple.get(1)", "1\t61\t1\t41", iterator.next().get( 1 ) );

    iterator.close();

    }

  public void testCoGroupSpillCustomWritable() throws Exception
    {
    if( !new File( inputFileLower ).exists() )
      fail( "data file not found" );

    copyFromLocal( inputFileLower );
    copyFromLocal( inputFileUpper );

    Tap sourceLower = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileLower );
    Tap sourceUpper = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileUpper );

    Map sources = new HashMap();

    sources.put( "lower", sourceLower );
    sources.put( "upper", sourceUpper );

    Function splitter = new RegexSplitter( new Fields( "num", "char" ), " " );

    // using null pos so all fields are written
    Tap sink = new Hfs( new SequenceFile( Fields.ALL ), outputPath + "/hadoop/customerwritable", true );

    Pipe pipeLower = new Each( new Pipe( "lower" ), new Fields( "line" ), splitter );
    pipeLower = new Each( pipeLower, new InsertTestText( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeLower = new Each( pipeLower, new InsertTestText( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );
    pipeLower = new Each( pipeLower, new InsertTestText( new Fields( "text" ), "inserted text as custom text" ), Fields.ALL );

    Pipe pipeUpper = new Each( new Pipe( "upper" ), new Fields( "line" ), splitter );
    pipeUpper = new Each( pipeUpper, new InsertTestText( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeUpper = new Each( pipeUpper, new InsertTestText( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );
    pipeUpper = new Each( pipeUpper, new InsertTestText( new Fields( "text" ), "inserted text as custom text" ), Fields.ALL );

    Pipe splice = new CoGroup( pipeLower, new Fields( "group" ), pipeUpper, new Fields( "group" ), Fields.size( 10 ) );

    Map<Object, Object> properties = getProperties();

    properties.put( CoGroupClosure.SPILL_THRESHOLD, 1 );
//    String serializations = MultiMapReducePlanner.getJobConf( properties ).get( "io.serializations" );
//    serializations = Util.join( ",", serializations, JavaSerialization.class.getName() );
//    System.out.println( "serializations = " + serializations );
//    MultiMapReducePlanner.getJobConf( properties ).set( "io.serializations",serializations );
    MultiMapReducePlanner.getJobConf( properties ).set( "io.serializations", TestSerialization.class.getName() );

    Flow countFlow = new FlowConnector( properties ).connect( sources, sink, splice );

//    countFlow.writeDOT( "cogroup.dot" );
//    System.out.println( "countFlow =\n" + countFlow );

    countFlow.complete();

    validateLength( countFlow, 25, null );
    }

  public void testCoGroupRawAsKeyValue() throws Exception
    {
    if( !new File( inputFileLower ).exists() )
      fail( "data file not found" );

    copyFromLocal( inputFileLower );
    copyFromLocal( inputFileUpper );

    Tap sourceLower = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileLower );
    Tap sourceUpper = new Hfs( new TextLine( new Fields( "offset", "line" ) ), inputFileUpper );

    Map sources = new HashMap();

    sources.put( "lower", sourceLower );
    sources.put( "upper", sourceUpper );

    Function splitter = new RegexSplitter( new Fields( "num", "char" ), " " );

    // using null pos so all fields are written
    Tap sink = new Hfs( new SequenceFile( Fields.ALL ), outputPath + "/hadoop/rawbyteskeyvalue", true );

    Pipe pipeLower = new Each( new Pipe( "lower" ), new Fields( "line" ), splitter );
    pipeLower = new Each( pipeLower, new InsertRawBytes( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeLower = new Each( pipeLower, new InsertRawBytes( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );

    Pipe pipeUpper = new Each( new Pipe( "upper" ), new Fields( "line" ), splitter );
    pipeUpper = new Each( pipeUpper, new InsertRawBytes( new Fields( "group" ), "inserted text as bytes" ), Fields.ALL );
    pipeUpper = new Each( pipeUpper, new InsertRawBytes( new Fields( "value" ), "inserted text as bytes" ), Fields.ALL );

    Fields groupFields = new Fields( "group" );
    groupFields.setComparator( "group", new BytesComparator() );

    Fields declaredFields = new Fields( "num", "char", "group", "value", "num2", "char2", "group2", "value2" );
    Pipe splice = new CoGroup( pipeLower, groupFields, pipeUpper, groupFields, declaredFields );

    // test sorting comparison
    Fields valueFields = new Fields( "value" );
    valueFields.setComparator( "value", new BytesComparator() );
    splice = new GroupBy( splice, groupFields, valueFields );

    Map<Object, Object> properties = getProperties();
    TupleSerialization.addSerialization( properties, BytesSerialization.class.getName() );
    Flow countFlow = new FlowConnector( properties ).connect( sources, sink, splice );

//    countFlow.writeDOT( "cogroup.dot" );
//    System.out.println( "countFlow =\n" + countFlow );

    countFlow.complete();

    validateLength( countFlow, 25, null );
    }

  }