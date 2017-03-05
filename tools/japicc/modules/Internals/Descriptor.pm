###########################################################################
# A module to handle XML descriptors
#
# Copyright (C) 2016 Andrey Ponomarenko's ABI Laboratory
#
# Written by Andrey Ponomarenko
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License or the GNU Lesser
# General Public License as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# and the GNU Lesser General Public License along with this program.
# If not, see <http://www.gnu.org/licenses/>.
###########################################################################
use strict;

sub createDesc($$)
{
    my ($Path, $LVer) = @_;
    
    if(not -e $Path) {
        return undef;
    }
    
    if(-d $Path or $Path=~/\.jar\Z/)
    {
        return "
            <version>
                ".$In::Desc{$LVer}{"TargetVersion"}."
            </version>
            
            <archives>
                $Path
            </archives>";
    }
    
    # standard XML-descriptor
    return readFile($Path);
}

sub readDesc($$)
{
    my ($Content, $LVer) = @_;
    
    if(not $Content) {
        exitStatus("Error", "XML descriptor is empty");
    }
    
    if($Content!~/\</) {
        exitStatus("Error", "descriptor should be one of the following: Java archive, XML descriptor, API dump or directory with Java archives.");
    }
    
    $Content=~s/\/\*(.|\n)+?\*\///g;
    $Content=~s/<\!--(.|\n)+?-->//g;
    $In::Desc{$LVer}{"Version"} = parseTag(\$Content, "version");
    
    if(defined $In::Desc{$LVer}{"TargetVersion"}) {
        $In::Desc{$LVer}{"Version"} = $In::Desc{$LVer}{"TargetVersion"};
    }
    
    if($In::Desc{$LVer}{"Version"} eq "") {
        exitStatus("Error", "version in the XML descriptor is not specified (<version> section)");
    }
    
    if(my $Archives = parseTag(\$Content, "archives"))
    {
        foreach my $Path (split(/\s*\n\s*/, $Archives))
        {
            if(not -e $Path) {
                exitStatus("Access_Error", "can't access \'$Path\'");
            }
            $Path = getAbsPath($Path);
            $In::Desc{$LVer}{"Archives"}{$Path} = 1;
        }
    }
    else {
        exitStatus("Error", "descriptor does not contain info about Java archives");
    }
    
    foreach my $Package (split(/\s*\n\s*/, parseTag(\$Content, "skip_packages"))) {
        $In::Desc{$LVer}{"SkipPackages"}{$Package} = 1;
    }
    foreach my $Package (split(/\s*\n\s*/, parseTag(\$Content, "packages"))) {
        $In::Desc{$LVer}{"KeepPackages"}{$Package} = 1;
    }
}

return 1;
