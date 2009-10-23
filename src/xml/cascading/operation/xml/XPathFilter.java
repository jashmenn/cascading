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

package cascading.operation.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import cascading.flow.FlowProcess;
import cascading.operation.Filter;
import cascading.operation.FilterCall;
import cascading.operation.OperationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * XPathFilter will filter out a Tuple if the given XPath expression returns false. Set removeMatch to true
 * if the filter should be reversed.
 */
public class XPathFilter extends XPathOperation implements Filter<DocumentBuilder>
  {
  /** Field LOG */
  private static final Logger LOG = Logger.getLogger( XPathFilter.class );

  /** Field removeMatch */
  private boolean removeMatch = false;

  /**
   * Constructor XPathFilter creates a new XPathFilter instance.
   *
   * @param namespaces of type String[][]
   * @param path       of type String
   */
  public XPathFilter( String[][] namespaces, String path )
    {
    super( 1, namespaces, path );
    }

  /**
   * Constructor XPathFilter creates a new XPathFilter instance.
   *
   * @param removeMatch of type boolean
   * @param namespaces  of type String[][]
   * @param path        of type String
   */
  public XPathFilter( boolean removeMatch, String[][] namespaces, String path )
    {
    super( 1, namespaces, path );
    this.removeMatch = removeMatch;
    }

  /** @see cascading.operation.Filter#isRemove(cascading.flow.FlowProcess,cascading.operation.FilterCall) */
  public boolean isRemove( FlowProcess flowProcess, FilterCall<DocumentBuilder> filterCall )
    {
    String argument = (String) filterCall.getArguments().getString( 0 );
    Document document = parseDocument( filterCall.getContext(), argument );
    XPathExpression expression = getExpressions().get( 0 );

    try
      {
      boolean value = (Boolean) expression.evaluate( document, XPathConstants.BOOLEAN );

      if( LOG.isDebugEnabled() )
        LOG.debug( "xpath: " + paths[ 0 ] + " matches: " + value );

      return value == removeMatch;
      }
    catch( XPathExpressionException exception )
      {
      throw new OperationException( "could not evaluate xpath expression: " + paths[ 0 ], exception );
      }
    }
  }