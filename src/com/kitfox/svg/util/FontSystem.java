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
 * Created on April 24, 2015
 */
package com.kitfox.svg.util;

import com.kitfox.svg.Font;
import com.kitfox.svg.FontFace;
import com.kitfox.svg.Glyph;
import com.kitfox.svg.MissingGlyph;
import java.awt.Canvas;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.util.HashMap;

/**
 *
 * @author kitfox
 */
public class FontSystem extends Font
{
    java.awt.Font sysFont;
    FontMetrics fm;

    HashMap glyphCache = new HashMap();
    
    public FontSystem(String fontFamily, int fontStyle, int fontWeight, int fontSize)
    {
        int style;
        switch (fontStyle)
        {
            case com.kitfox.svg.Text.TXST_ITALIC:
                style = java.awt.Font.ITALIC;
                break;
            default:
                style = java.awt.Font.PLAIN;
                break;
        }

        int weight;
        switch (fontWeight)
        {
            case com.kitfox.svg.Text.TXWE_BOLD:
            case com.kitfox.svg.Text.TXWE_BOLDER:
                weight = java.awt.Font.BOLD;
                break;
            default:
                weight = java.awt.Font.PLAIN;
                break;
        }
        sysFont = new java.awt.Font(fontFamily, style | weight, (int) fontSize);
        
        Canvas c = new Canvas();
        fm = c.getFontMetrics(sysFont);
        
        FontFace face = new FontFace();
        face.setAscent(fm.getAscent());
        face.setDescent(fm.getDescent());
        face.setUnitsPerEm(fm.charWidth('M'));
        setFontFace(face);
    }

    public MissingGlyph getGlyph(String unicode)
    {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector vec = sysFont.createGlyphVector(frc, unicode);
        
        Glyph glyph = (Glyph)glyphCache.get(unicode);
        if (glyph == null)
        {
            glyph = new Glyph();
            glyph.setPath(vec.getGlyphOutline(0));

            GlyphMetrics gm = vec.getGlyphMetrics(0);
            glyph.setHorizAdvX((int)gm.getAdvanceX());
            glyph.setVertAdvY((int)gm.getAdvanceY());
            glyph.setVertOriginX(0);
            glyph.setVertOriginY(0);
            
            glyphCache.put(unicode, glyph);
        }
        
        return glyph;
    }
    
    
}
