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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chromattic.ext.ntdef.NTFolder;
import org.chromattic.ext.ntdef.NTHierarchyNode;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webos.services.desktop.DesktopBackground;
import org.exoplatform.webos.services.desktop.DesktopBackgroundService;
import org.exoplatform.webos.services.desktop.exception.ImageQuantityException;
import org.exoplatform.webos.services.desktop.exception.ImageSizeException;

/**
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 * Sep 14, 2010
 */

public class DesktopBackgroundServiceImpl implements DesktopBackgroundService
{
   private static final Log log = ExoLogger.getExoLogger("portal:DesktopBackgroundServiceImpl");

   private ChromatticManager chromatticManager;

   private ChromatticLifeCycle chromatticLifecycle;

   private DataStorage dataStorage;

   // 0 means unlimited
   private int quantityLimit;

   // 0 means unlimited
   //This is applied for each image
   private int sizeLimit;

   public DesktopBackgroundServiceImpl(ChromatticManager manager, DataStorage dataStorage,  InitParams params) throws Exception
   {
      chromatticManager = manager;
      chromatticLifecycle = manager.getLifeCycle("mop");

      this.dataStorage = dataStorage;

      if (params != null)
      {
         ValueParam quantityParam = params.getValueParam("image.limit.quantity");
         if (quantityParam != null)
         {
            quantityLimit = Integer.parseInt(quantityParam.getValue());
         }
         ValueParam sizeParam = params.getValueParam("image.limit.size");
         if (sizeParam != null)
         {
            sizeLimit = Integer.parseInt(sizeParam.getValue());
         }
      }
   }

/*
   public DesktopBackgroundRegistry initBackgroundRegistry()
   {
      DesktopBackgroundRegistry backgroundRegistry;
      Chromattic chromattic = chromatticLifecycle.getChromattic();
      ChromatticSession session = chromattic.openSession();

      backgroundRegistry = session.findByPath(DesktopBackgroundRegistry.class, "webos:desktopBackgroundRegistry");
      if (backgroundRegistry == null)
      {
         backgroundRegistry = session.insert(DesktopBackgroundRegistry.class, "webos:desktopBackgroundRegistry");
         session.save();
      }
      
      return backgroundRegistry; 
   }
*/

/*
   public ChromatticLifeCycle getChromatticLifecycle()
   {
      return this.chromatticLifecycle;
   }
*/

   public int getSizeLimit()
   {
      return sizeLimit;
   }

   public boolean removeBackgroundImage(String userName, String backgroundImageName) throws Exception
   {
      PersonalBackgroundSpace space = getSpace(userName, false);
      if (space == null)
      {
         //TODO: Throws an exception here
         return false;
      }

      if (backgroundImageName !=null)
      {
         if (space.getBackgroundImageFolder().getChild(backgroundImageName) == null)
         {
            throw new IllegalStateException("Image doesn't exists");
         }
      }
      space.getBackgroundImageFolder().getChildren().remove(backgroundImageName);
      return true;
   }

   private PersonalBackgroundSpace getSpace(String userName, boolean create) throws Exception
   {
      if (userName == null)
      {
         return null;
      }
      PortalConfig cfg = dataStorage.getPortalConfig("user", userName);
      PersonalBackgroundSpace space = dataStorage.adapt(cfg, PersonalBackgroundSpace.class, create);
      if (space != null)
      {
         NTFolder folder = space.getBackgroundImageFolder();
         if (folder == null && create) {
            folder = space.createFolder();
            space.setBackgroundImageFolder(folder);
            space.uploadDefaultBackgroundImage();
         }
      }
      return space;
   }

   public boolean uploadBackgroundImage(String userName, String backgroundImageName, String mimeType, String encoding,
         InputStream binaryStream) throws Exception
   {
     if (userName == null || backgroundImageName == null || mimeType == null || encoding == null || binaryStream == null)
     {
        throw new IllegalArgumentException("One of the arguments is null");
     }

     //
     PersonalBackgroundSpace space = getSpace(userName, true);
     NTFolder folder = space.getBackgroundImageFolder();
     Map<String,NTHierarchyNode> children = folder.getChildren();
     if (quantityLimit != 0 && children.size() == quantityLimit)
     {
        log.debug("Each user can only have" + quantityLimit + " background images");
        throw new ImageQuantityException(quantityLimit);
     }
     if (sizeLimit != 0 && sizeLimit < binaryStream.available()/1024.0/1024)
     {
        log.debug("Can't upload, naximum image size is :" + sizeLimit);
        throw new ImageSizeException(sizeLimit, backgroundImageName);
     }

     backgroundImageName = processDuplicatedName(space, backgroundImageName);
     return space.uploadBackgroundImage(backgroundImageName, mimeType, encoding, binaryStream);
   }

   private String processDuplicatedName(PersonalBackgroundSpace space, String imgName)
   {
      int dotIndex = imgName.lastIndexOf(".");
      if (dotIndex == -1)
      {
         dotIndex = imgName.length();
      }
      StringBuilder nameBuilder = new StringBuilder(imgName).insert(dotIndex, "(0)");

      int idx = 0;
      while (space.getBackgroundImageFolder().getChild(imgName) != null)
      {
         nameBuilder.replace(dotIndex + 1, nameBuilder.indexOf(")", dotIndex), String.valueOf(idx++));
         imgName = nameBuilder.toString();
      }
      return imgName;
   }

   public DesktopBackground getCurrentDesktopBackground(String pageID) throws Exception
   {
      if(pageID == null)
      {
         return null;
      }

      Page desktopPage = dataStorage.getPage(pageID);
      if (desktopPage == null)
      {
         throw new IllegalStateException("page : " + pageID + " doen't exists");
      }
      DesktopPageMetadata pageMetadata = dataStorage.adapt(desktopPage, DesktopPageMetadata.class);
      String selectedBackground = pageMetadata.getBackgroundImage();
      
      if (selectedBackground != null)
      {
         return new DesktopBackground(makeImageURL(parsePageID(pageID), selectedBackground), selectedBackground);
      }
      return null;
   }

   public void setSelectedBackgroundImage(String pageID, String imageName) throws Exception
   {
      Page desktopPage = dataStorage.getPage(pageID);
      if (desktopPage == null)
      {
         throw new IllegalStateException("page : " + pageID + " doen't exists");
      }
      DesktopPageMetadata pageMetadata = dataStorage.adapt(desktopPage, DesktopPageMetadata.class);

      String userName = parsePageID(pageID);
      PersonalBackgroundSpace space = getSpace(userName, true);
      boolean imgDeleted = false;
      if (imageName !=null && space.getBackgroundImageFolder().getChild(imageName) == null)
      {
         imageName = null;
         imgDeleted = true;
      }
      pageMetadata.setBackgroundImage(imageName);
      dataStorage.save(desktopPage);
      if (imgDeleted)
      {
         throw new IllegalStateException("Image doesn't exists");
      }
   }

   private String parsePageID(String pageID)
   {
      String[] idFrags = pageID.split("::");
      if (idFrags.length < 3)
      {
         throw new IllegalArgumentException("Can't parse pageID :" + pageID);
      }
      return idFrags[1];
   }

   public List<DesktopBackground> getUserDesktopBackgrounds(String userName) throws Exception
   {
      PersonalBackgroundSpace space = getSpace(userName, true);
      List<DesktopBackground> backgroundList = new ArrayList<DesktopBackground>();
      if (space != null)
      {
         NTFolder backgroundFolder = space.getBackgroundImageFolder();
         if (backgroundFolder != null)
         {
            Set<String> availableBackgrounds = backgroundFolder.getChildren().keySet();
            for(String background : availableBackgrounds)
            {
               backgroundList.add(new DesktopBackground(makeImageURL(userName, background), background));
            }
         }
      }

      //
      return backgroundList;
   }

   public DesktopBackground getUserDesktopBackground(String userName, String imageName) throws Exception
   {
      if (imageName == null)
      {
         return null;
      }
      
      PersonalBackgroundSpace space = getSpace(userName, true);
      if (space == null)
      {
         throw new IllegalStateException("Can't found PersonalBackgroundSpace for :" + userName);
      }
      NTFolder backgroundFolder = space.getBackgroundImageFolder();
      if (backgroundFolder.getChildren().containsKey(imageName))
      {
         return new DesktopBackground(makeImageURL(userName, imageName), imageName);
      }
      return null;      
   }

   private String makeImageURL(String userName, String imageLabel)
   {
      StringBuilder urlBuilder = new StringBuilder("/");
      urlBuilder.append(PortalContainer.getCurrentPortalContainerName()).append("/rest/jcr/");
      urlBuilder.append(chromatticLifecycle.getRepositoryName()).append("/");
      urlBuilder.append(chromatticLifecycle.getWorkspaceName()).append("/production/mop:workspace/mop:usersites/mop:");
      urlBuilder.append(userName).append("/webos:personalBackgroundFolder/").append(imageLabel);

      return urlBuilder.toString();
   }
}
