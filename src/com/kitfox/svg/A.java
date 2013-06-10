/*
 * SVG Salamander
 * Copyright (c) 2004, Mark McKay
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   - Redistributions of source code must retain the above 
 *     copyright notice, this list of conditions and the following
 *     disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials 
 *     provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 * projects can be found at http://www.kitfox.com
 *
 * Created on January 26, 2004, 1:56 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.net.URI;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class A extends Group
{
    public static final String TAG_NAME = "a";

    URI href;
    String title;

    /** Creates a new instance of Stop */
    public A()
    {
    }

    public String getTagName()
    {
        return TAG_NAME;
    }
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("xlink:href")))
        {
            href = sty.getURIValue(getXMLBase());
        }
        
        if (getPres(sty.setName("xlink:title")))
        {
            title = sty.getStringValue();
        }
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
        boolean changeState = super.updateTime(curTime);
//        if (trackManager.getNumTracks() == 0) return false;

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("xlink:href")))
        {
            href = sty.getURIValue(getXMLBase());
        }
        
        if (getPres(sty.setName("xlink:title")))
        {
            title = sty.getStringValue();
        }
        
        return changeState;
    }
}
