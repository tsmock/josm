// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;

/**
 * Read out exif file information from a jpeg file
 * @author Imi
 */
public class ExifReader {

    @SuppressWarnings("unchecked") public static Date readTime(File filename) throws ParseException {
        Date date = null;
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(filename);
            for (Iterator<Directory> dirIt = metadata.getDirectoryIterator(); dirIt.hasNext();) {
                for (Iterator<Tag> tagIt = dirIt.next().getTagIterator(); tagIt.hasNext();) {
                    Tag tag = tagIt.next();
                    if (tag.getTagType() == 0x9003)
                        return DateParser.parse(tag.getDescription());
                    if (tag.getTagType() == 0x132 || tag.getTagType() == 0x9004)
                        date = DateParser.parse(tag.getDescription());
                }
            }
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    @SuppressWarnings("unchecked") public static Integer readOrientation(File filename) throws ParseException {
        Integer orientation = null;
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final Directory dir = metadata.getDirectory(ExifDirectory.class);
            orientation = dir.getInt(ExifDirectory.TAG_ORIENTATION);
        } catch (JpegProcessingException e) {
            e.printStackTrace();
        } catch (MetadataException e) {
            e.printStackTrace();
        }
        return orientation;
    }

}
