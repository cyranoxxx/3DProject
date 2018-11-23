/*****************************************************************************
 * STLParser.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 ****************************************************************************/

package com.andresoviedo.android_3d_model_engine.services.stl;

// External imports

import java.io.IOException;
import java.net.URL;
import java.util.List;

// Local imports


abstract class STLParser
{
    protected int       itsNumOfObjects = 0;
    protected int[]    itsNumOfFacets = null;
    protected String[] itsNames = null;


    protected boolean strictParsing;


    protected List<String> parsingMessages;

    public STLParser()
    {
        this(false);
    }


    public STLParser(boolean strict)
    {
        strictParsing = strict;
    }


    String[] getObjectNames()
    {
        return itsNames;
    }


    int[] getNumOfFacets()
    {
        return itsNumOfFacets;
    }


    int getNumOfObjects()
    {
        return itsNumOfObjects;
    }


    public List<String> getParsingMessages()
    {
        return parsingMessages;
    }


    abstract void close() throws IOException;


    abstract boolean parse(URL url)
        throws IOException;


    abstract boolean parse(URL url, Component parentComponent)
        throws IllegalArgumentException, IOException;


    abstract boolean getNextFacet(double[] normal, double[][] vertices)
        throws IllegalArgumentException, IOException;
}