/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.webos.services.desktop.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.chromattic.api.annotations.Create;
import org.chromattic.api.annotations.FormattedBy;
import org.chromattic.api.annotations.MappedBy;
import org.chromattic.api.annotations.Name;
import org.chromattic.api.annotations.OneToOne;
import org.chromattic.api.annotations.Owner;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;
import org.chromattic.common.IO;
import org.chromattic.ext.ntdef.NTFolder;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.container.PortalContainer;
import javax.servlet.ServletContext;

/**
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 * Sep 14, 2010
 */

@PrimaryType(name = "webos:personalBackgroundSpace")
@FormattedBy(WebOSChromatticFormatter.class)
public abstract class PersonalBackgroundSpace
{

   @Name
   public abstract String getName();
   
   @Property(name = "webos:title")
   public abstract String getTitle();
   
   public abstract void setTitle(String title);
   
   @Create
   public abstract NTFolder createBackgroundImageFolder();
   
   @OneToOne
   @Owner
   @MappedBy("webos:personalBackgroundFolder")
   public abstract NTFolder getBackgroundImageFolder();
   
   public abstract void setBackgroundImageFolder(NTFolder backgroundImageFolder);
   
   public boolean uploadBackgroundImage(String imageName, String mimeType, String encoding, InputStream binaryStream)
   {
	   try{
		   NTFolder imageFolder = getBackgroundImageFolder();
		   byte[] content = IO.getBytes(binaryStream);
		   imageFolder.createFile(imageName, new Resource(mimeType, encoding, content));
		   binaryStream.close();
		   return true;
	   }catch(Exception ex)
	   {
		   ex.printStackTrace();
		   return false;
	   }
   }
      
   protected void uploadDefaultBackgroundImage()
   {
      PortalContainer pcontainer = PortalContainer.getInstance();
      ServletContext mergedContext = pcontainer.getPortalContext();

      Set<String> defaultImages = mergedContext.getResourcePaths("/backgrounds/");

      if(defaultImages == null)
      {
         return;
      }

      for(String defaultImage : defaultImages)
      {
         if(defaultImage.endsWith(".jpg"))
         {
            String displayName = defaultImage.substring("/backgrounds/".length());

            try{
               uploadBackgroundImage(displayName, "image/jpeg", "UTF-8", mergedContext.getResourceAsStream(defaultImage));
            }
            catch(Exception ex)
            {
               //ex.printStackTrace();
            }
         }
      }
   }
}
