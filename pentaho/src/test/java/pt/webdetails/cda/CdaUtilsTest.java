/*!
 * Copyright 2002 - 2018 Webdetails, a Hitachi Vantara company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package pt.webdetails.cda;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import pt.webdetails.cda.exporter.ExportedQueryResult;
import pt.webdetails.cda.services.Previewer;
import pt.webdetails.cda.settings.CdaSettingsReadException;
import pt.webdetails.cda.utils.DoQueryParameters;
import pt.webdetails.cda.utils.QueryParameters;
import pt.webdetails.cpf.messaging.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Khayrutdinov
 */
public class CdaUtilsTest {

  @Test
  public void previewQuery_ValidPath() throws Exception {
    CdaUtils utils = spyUtilsWithFakePreviewer( "file.cda", "qwerty" );
    doNothing().when( utils ).checkFileExists( "file.cda" );

    assertEquals( "qwerty", utils.previewQuery( mockRequest( "file.cda" ) ) );
  }

  @Test( expected = CdaSettingsReadException.class )
  public void previewQuery_InvalidPath() throws Exception {
    CdaUtils utils = spyUtilsWithFakePreviewer( "file.cda", "qwerty" );
    doThrow( new CdaSettingsReadException( "", null ) ).when( utils ).checkFileExists( "file.cda" );

    assertEquals( "qwerty", utils.previewQuery( mockRequest( "file.cda" ) ) );
  }

  @Test
  public void testDoQueryInterPlugin() throws Exception {
    QueryParameters queryParametersUtil = spy( new QueryParameters() );
    ExportedQueryResult exportedQueryResult = mock( ExportedQueryResult.class );
    doReturn( "exported query result" ).when( exportedQueryResult ).asString();
    CdaUtils utils = spyUtilsWithFakePreviewer( "file.cda", "qwerty" );
    utils.setQueryParametersUtil( queryParametersUtil );
    doReturn( exportedQueryResult).when( utils ).doQueryInternal( any( DoQueryParameters.class ) );

    HttpServletRequest servletRequest = mock( HttpServletRequest.class );
    Enumeration enumerationParameterNames = Collections.enumeration( Arrays.asList( "param1", "param2", "param3" ) );
    doReturn( enumerationParameterNames ).when( servletRequest ).getParameterNames();
    String[] parameterValueFirst = new String[] { "val11", "val12" };
    doReturn( parameterValueFirst ).when( servletRequest ).getParameterValues( "param1" );
    String[] parameterValueSecond = new String[] { "val21", "val22" };
    doReturn( parameterValueSecond ).when( servletRequest ).getParameterValues( "param2" );
    String[] parameterValueThird = new String[] { "val31" };
    doReturn( parameterValueThird ).when( servletRequest ).getParameterValues( "param3" );

    //the actual testing call
    String result = utils.doQueryInterPlugin( servletRequest );
    assertEquals( "exported query result", result);

    ArgumentCaptor<Map> argumentCaptorParams = ArgumentCaptor.forClass( Map.class );
    verify( queryParametersUtil ).getDoQueryParameters( argumentCaptorParams.capture() );

    ArgumentCaptor<DoQueryParameters> argumentCaptorDoQueryParams = ArgumentCaptor.forClass( DoQueryParameters.class );
    verify( utils ).doQueryInternal( argumentCaptorDoQueryParams.capture() );

    assertNotNull( argumentCaptorParams.getValue() );
    assertEquals( 3, argumentCaptorParams.getValue().size() );
    assertNotNull( argumentCaptorParams.getValue().get( "param1" ) );
    assertTrue( argumentCaptorParams.getValue().get( "param1" ) instanceof List );
    assertEquals( 2, ( ( List<String> ) argumentCaptorParams.getValue().get( "param1" ) ).size() );
    assertTrue( ( ( List<String> ) argumentCaptorParams.getValue().get( "param1" ) ).contains( "val11" ) );
    assertTrue( ( ( List<String> ) argumentCaptorParams.getValue().get( "param1" ) ).contains( "val12" ) );
    assertNotNull( argumentCaptorParams.getValue().get( "param2" ) );
    assertTrue( argumentCaptorParams.getValue().get( "param2" ) instanceof List );
    assertEquals( 2, ( ( List<String> ) argumentCaptorParams.getValue().get( "param2" ) ).size() );
    assertTrue( ( ( List<String> ) argumentCaptorParams.getValue().get( "param2" ) ).contains( "val21" ) );
    assertTrue( ( ( List<String> ) argumentCaptorParams.getValue().get( "param2" ) ).contains( "val22" ) );
    assertNotNull( argumentCaptorParams.getValue().get( "param3" ) );
    assertTrue( argumentCaptorParams.getValue().get( "param3" ) instanceof List );
    assertEquals( 1, ( ( List<String> ) argumentCaptorParams.getValue().get( "param3" ) ).size() );
    assertTrue( ( ( List<String> ) argumentCaptorParams.getValue().get( "param3" ) ).contains( "val31" ) );

    verify( utils, times( 1 ) ).doQueryInternal( argumentCaptorDoQueryParams.getValue() );
  }

  private CdaUtils spyUtilsWithFakePreviewer( String file, String content ) throws Exception {
    Previewer previewer = mock( Previewer.class );
    when( previewer.previewQuery( file ) ).thenReturn( content );

    CdaUtils utils = spy( new CdaUtils() );
    doReturn( previewer ).when( utils ).getPreviewer();
    return utils;
  }

  private HttpServletRequest mockRequest( String file ) {
    return new MockHttpServletRequest( "/previewQuery", Collections.singletonMap( "path", new String[] { file } ) );
  }
}
