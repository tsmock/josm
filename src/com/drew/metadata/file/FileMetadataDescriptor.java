/*
 * Copyright 2002-2015 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */
package com.drew.metadata.file;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;
import com.drew.metadata.TagDescriptor;

/**
 * @author Drew Noakes https://drewnoakes.com
 */
public class FileMetadataDescriptor extends TagDescriptor<FileMetadataDirectory>
{
    public FileMetadataDescriptor(@NotNull FileMetadataDirectory directory)
    {
        super(directory);
    }

    @Override
    @Nullable
    public String getDescription(int tagType)
    {
        switch (tagType) {
            case FileMetadataDirectory.TAG_FILE_SIZE:
                return getFileSizeDescription();
            default:
                return super.getDescription(tagType);
        }
    }

    @Nullable
    private String getFileSizeDescription()
    {
        Long size = _directory.getLongObject(FileMetadataDirectory.TAG_FILE_SIZE);

        if (size == null)
            return null;

        return Long.toString(size) + " bytes";
    }
}

