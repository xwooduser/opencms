
/*
* File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsRename.java,v $
* Date   : $Date: 2001/01/24 09:43:30 $
* Version: $Revision: 1.34 $
*
* Copyright (C) 2000  The OpenCms Group 
* 
* This File is part of OpenCms -
* the Open Source Content Mananagement System
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.com
* 
* You should have received a copy of the GNU General Public License
* long with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * Template class for displaying the rename screen of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * @author Michael Emmerich
 * @author Michaela Schleich
 * @version $Revision: 1.34 $ $Date: 2001/01/24 09:43:30 $
 */

public class CmsRename extends CmsWorkplaceDefault implements I_CmsWpConstants,I_CmsConstants {
    
    /**
     * This method changes the path of the body file in the xml conten file
     * if file type name is page
     * 
     * @param cms The CmsObject
     * @param file The XML content file
     * @param bodypath the new XML content entry
     * @exception Exception if something goes wrong.
     */
    
    private void changeContent(CmsObject cms, CmsFile file, String bodypath) throws CmsException {
        file = cms.readFile(file.getAbsolutePath());
        CmsXmlControlFile hXml = new CmsXmlControlFile(cms, file);
        hXml.setElementTemplate("body", bodypath);
        hXml.write();
    }
    
    /**
     * This method checks if all nescessary folders are exisitng in the content body
     * folder and creates the missing ones. <br>
     * All page contents files are stored in the content body folder in a mirrored directory
     * structure of the OpenCms filesystem. Therefor it is nescessary to create the 
     * missing folders when a new page document is createg.
     * @param cms The CmsObject
     * @param path The path in the CmsFilesystem where the new page should be created.
     * @exception CmsException if something goes wrong.
     */
    
    private void checkFolders(CmsObject cms, String path) throws CmsException {
        String completePath = C_CONTENTBODYPATH;
        StringTokenizer t = new StringTokenizer(path, "/");
        
        // check if all folders are there
        while(t.hasMoreTokens()) {
            String foldername = t.nextToken();
            try {
                
                // try to read the folder. if this fails, an exception is thrown  
                cms.readFolder(completePath + foldername + "/");
            }
            catch(CmsException e) {
                
                // the folder could not be read, so create it.
                String orgFolder = completePath + foldername + "/";
                orgFolder = orgFolder.substring(C_CONTENTBODYPATH.length() - 1);
                CmsFolder newfolder = cms.createFolder(completePath, foldername);
                CmsFolder folder = cms.readFolder(orgFolder);
                cms.lockResource(newfolder.getAbsolutePath());
                cms.chown(newfolder.getAbsolutePath(), cms.readOwner(folder).getName());
                cms.chgrp(newfolder.getAbsolutePath(), cms.readGroup(folder).getName());
                cms.chmod(newfolder.getAbsolutePath(), folder.getAccessFlags());
                cms.unlockResource(newfolder.getAbsolutePath());
            }
            completePath += foldername + "/";
        }
    }
    
    /**
     * Gets all resources - files and subfolders - of a given folder.
     * @param cms The CmsObject.
     * @param rootFolder The name of the given folder.
     * @param allFiles Vector containing all files found so far. All files of this folder
     * will be added here as well.
     * @param allolders Vector containing all folders found so far. All subfolders of this folder
     * will be added here as well.
     * @exception Throws CmsException if something goes wrong.
     */
    
    private void getAllResources(CmsObject cms, String rootFolder, Vector allFiles, Vector allFolders) throws CmsException {
        Vector folders = new Vector();
        Vector files = new Vector();
        
        // get files and folders of this rootFolder
        folders = cms.getSubFolders(rootFolder);
        files = cms.getFilesInFolder(rootFolder);
        
        //copy the values into the allFiles and allFolders Vectors
        for(int i = 0;i < folders.size();i++) {
            allFolders.addElement((CmsFolder)folders.elementAt(i));
            getAllResources(cms, ((CmsFolder)folders.elementAt(i)).getAbsolutePath(), allFiles, allFolders);
        }
        for(int i = 0;i < files.size();i++) {
            allFiles.addElement((CmsFile)files.elementAt(i));
        }
    }
    
    /**
     * method to check get the real body path from the content file
     * 
     * @param cms The CmsObject, to access the XML read file.
     * @param file File in which the body path is stored.
     */
    
    private String getBodyPath(CmsObject cms, CmsFile file) throws CmsException {
        file = cms.readFile(file.getAbsolutePath());
        CmsXmlControlFile hXml = new CmsXmlControlFile(cms, file);
        return hXml.getElementTemplate("body");
    }
    
    /**
     * Overwrites the getContent method of the CmsWorkplaceDefault.<br>
     * Gets the content of the rename template and processed the data input.
     * @param cms The CmsObject.
     * @param templateFile The lock template file
     * @param elementName not used
     * @param parameters Parameters of the request and the template.
     * @param templateSelector Selector of the template tag to be displayed.
     * @return Bytearre containgine the processed data of the template.
     * @exception Throws CmsException if something goes wrong.
     */
    
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, 
            Hashtable parameters, String templateSelector) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);
        CmsXmlWpTemplateFile xmlTemplateDocument = new CmsXmlWpTemplateFile(cms, templateFile);
        
        // the template to be displayed
        String template = null;
        
        // clear session values on first load
        String initial = (String)parameters.get(C_PARA_INITIAL);
        if(initial != null) {
            
            // remove all session values
            session.removeValue(C_PARA_FILE);
            session.removeValue(C_PARA_NAME);
            session.removeValue("lasturl");
        }
        
        // get the lasturl parameter
        String lasturl = getLastUrl(cms, parameters);
        
        // TODO: check, if this is neede: String lock=(String)parameters.get(C_PARA_LOCK);
        String filename = (String)parameters.get(C_PARA_FILE);
        if(filename != null) {
            session.putValue(C_PARA_FILE, filename);
        }
        filename = (String)session.getValue(C_PARA_FILE);
        String newFile = (String)parameters.get(C_PARA_NAME);
        if(session.getValue(C_PARA_NAME) != null) {
            if(newFile != null) {
                
                // Save the new parameter of the new filename.                
                // Only do this, if the session value already exists.                
                // We use the existance of a session value as a flag                
                // For initial try / retry after exception.
                session.putValue(C_PARA_NAME, newFile);
            }
            else {
                
                // Get back the saved value (if one exists)
                newFile = (String)session.getValue(C_PARA_NAME);
            }
        }
        
        //newFile=(String)session.getValue(C_PARA_NAME);
        String action = (String)parameters.get("action");
        CmsResource file = (CmsResource)cms.readFileHeader(filename);
        if(file.isFile()) {
            template = "file";
        }
        else {
            template = "folder";
        }
        
        //check if the name parameter was included in the request        
        // if not, the lock page is shown for the first time            
        //if (newFile == null) {
        if(newFile == null || session.getValue(C_PARA_NAME) == null) {
            if(newFile == null) {
                session.putValue(C_PARA_NAME, file.getName());
            }
            else {
                session.putValue(C_PARA_NAME, newFile);
            }
        }
        else {
            if(action == null) {
                template = "wait";
            }
            else {
                
                // now check if the resource is a file or a folder            
                if(file.isFile()) {
                    
                    // this is a file, so rename it         
                    try {
                        renameFile(cms, file, newFile);
                    }
                    catch(CmsException ex) {
                        
                        // something went wrong, so remove all session parameters
                        session.removeValue(C_PARA_FILE);
                        session.removeValue(C_PARA_NAME);
                        throw ex;
                    }
                    
                    // everything is done, so remove all session parameters
                    session.removeValue(C_PARA_FILE);
                    session.removeValue(C_PARA_NAME);
                    try {
                        if(lasturl == null || "".equals(lasturl)) {
                            cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceActionPath() 
                                    + C_WP_EXPLORER_FILELIST);
                        }
                        else {
                            cms.getRequestContext().getResponse().sendRedirect(lasturl);
                        }
                    }
                    catch(Exception e) {
                        throw new CmsException("Redirect fails :" + getConfigFile(cms).getWorkplaceActionPath() 
                                + C_WP_EXPLORER_FILELIST, CmsException.C_UNKNOWN_EXCEPTION, e);
                    }
                    return null;
                }
                else {
                    
                    // this is a folder                    
                    // get all subfolders and files
                    Vector allFolders = new Vector();
                    Vector allFiles = new Vector();
                    getAllResources(cms, filename, allFiles, allFolders);
                    String parent = file.getParent();
                    try {
                        if(!cms.accessWrite(filename)) {
                            throw new CmsException(filename, CmsException.C_NO_ACCESS);
                        }
                        
                        // first create the new folder
                        cms.copyFolder(filename, parent + newFile + "/");
                        
                        // then copy all folders
                        for(int i = 0;i < allFolders.size();i++) {
                            CmsFolder folder = (CmsFolder)allFolders.elementAt(i);
                            if(folder.getState() != C_STATE_DELETED) {
                                String newname = parent + newFile + "/" + folder.getAbsolutePath().substring(file.getAbsolutePath().length());
                                cms.copyFolder(folder.getAbsolutePath(), newname);
                            }
                        }
                        
                        // now move the files
                        for(int i = 0;i < allFiles.size();i++) {
                            CmsFile newfile = (CmsFile)allFiles.elementAt(i);
                            if(newfile.getState() != C_STATE_DELETED) {
                                String newname = parent + newFile + "/" + newfile.getAbsolutePath().substring(file.getAbsolutePath().length());
                                moveFile(cms, newfile, newname, "true");
                            }
                        }
                        
                        // finally remove the original folders
                        for(int i = 0;i < allFolders.size();i++) {
                            CmsFolder folder = (CmsFolder)allFolders.elementAt(allFolders.size() - i - 1);
                            if(folder.getState() != C_STATE_DELETED) {
                                cms.deleteFolder(folder.getAbsolutePath());
                                try {
                                    cms.deleteFolder(C_CONTENTBODYPATH + folder.getAbsolutePath().substring(1));
                                }
                                catch(CmsException e) {
                                    
                                }
                            }
                        }
                        
                        // as the last step, delete the original folder in the content folder
                        cms.deleteFolder(filename);
                        try {
                            cms.deleteFolder(C_CONTENTBODYPATH + filename.substring(1));
                        }
                        catch(CmsException e) {
                            
                        }
                        try {
                            cms.unlockResource(C_CONTENTBODYPATH + filename.substring(1));
                        }
                        catch(CmsException e) {
                            
                        }
                    }
                    catch(CmsException ex) {
                        
                        // something went wrong, so remove all session parameters                        
                        //session.removeValue(C_PARA_FILE); don't delete this. We really need this to try again.
                        session.removeValue(C_PARA_NAME);
                        throw ex;
                    }
                    
                    // everything is done, so remove all session parameters
                    session.removeValue(C_PARA_FILE);
                    session.removeValue(C_PARA_NAME);
                    xmlTemplateDocument.setData("lasturl", lasturl);
                    template = "update";
                }
            }
        }
        
        // set the required datablocks
        if(action == null) {
            String title = cms.readProperty(file.getAbsolutePath(), C_PROPERTY_TITLE);
            if(title == null) {
                title = "";
            }
            CmsXmlLanguageFile lang = xmlTemplateDocument.getLanguageFile();
            CmsUser owner = cms.readOwner(file);
            xmlTemplateDocument.setData("TITLE", title);
            xmlTemplateDocument.setData("STATE", getState(cms, file, lang));
            xmlTemplateDocument.setData("OWNER", Utils.getFullName(owner));
            xmlTemplateDocument.setData("GROUP", cms.readGroup(file).getName());
            xmlTemplateDocument.setData("FILENAME", file.getName());
        }
        
        // process the selected template 
        return startProcessing(cms, xmlTemplateDocument, "", parameters, template);
    }
    
    /**
     * Gets a formated file state string.
     * @param cms The CmsObject.
     * @param file The CmsResource.
     * @param lang The content definition language file.
     * @return Formated state string.
     */
    
    private String getState(CmsObject cms, CmsResource file, CmsXmlLanguageFile lang) throws CmsException {
        StringBuffer output = new StringBuffer();
        if(file.inProject(cms.getRequestContext().currentProject())) {
            int state = file.getState();
            output.append(lang.getLanguageValue("explorer.state" + state));
        }
        else {
            output.append(lang.getLanguageValue("explorer.statenip"));
        }
        return output.toString();
    }
    
    /**
     * Indicates if the results of this class are cacheable.
     * 
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file 
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }
    
    /**
     * Move a file to another folder.
     * If the file is a page, the content will be moved too.
     * @param cms The CmsObject.
     * @param file The file to be moved.
     * @param newFolder The folder the file has to be moved to.
     * @param flags Flags that indicate if the access flags have to be set to the default values.
     */
    
    private void moveFile(CmsObject cms, CmsFile file, String newFolder, String flags) throws CmsException {
        if((cms.getResourceType(file.getType()).getResourceName()).equals(C_TYPE_PAGE_NAME)) {
            String bodyPath = getBodyPath(cms, file);
            int help = C_CONTENTBODYPATH.lastIndexOf("/");
            String hbodyPath = (C_CONTENTBODYPATH.substring(0, help)) + (file.getAbsolutePath());
            if(hbodyPath.equals(bodyPath)) {
                String parent = newFolder.substring(0, newFolder.lastIndexOf("/") + 1);
                checkFolders(cms, parent);
                cms.moveFile((C_CONTENTBODYPATH.substring(0, help)) + file.getAbsolutePath(), 
                        (C_CONTENTBODYPATH.substring(0, help)) + newFolder);
                if(flags.equals("false")) {
                    
                    // set access flags of the new file to the default flags                    
                    //CmsFile newfile=cms.readFile(newFolder,file.getName());
                    CmsFile newfile = cms.readFile((C_CONTENTBODYPATH.substring(0, help)) + newFolder, file.getName());
                    Hashtable startSettings = null;
                    Integer accessFlags = null;
                    startSettings = (Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
                    if(startSettings != null) {
                        accessFlags = (Integer)startSettings.get(C_START_ACCESSFLAGS);
                    }
                    if(accessFlags == null) {
                        accessFlags = new Integer(C_ACCESS_DEFAULT_FLAGS);
                    }
                    newfile.setAccessFlags(accessFlags.intValue());
                    cms.writeFile(newfile);
                }
                
                //changeContent(cms, file, (C_CONTENTBODYPATH.substring(0,help))+newFolder+file.getName());
                changeContent(cms, file, (C_CONTENTBODYPATH.substring(0, help)) + newFolder);
            }
        }
        
        // moves the file and set the access flags if nescessary
        cms.moveFile(file.getAbsolutePath(), newFolder);
        if(flags.equals("false")) {
            
            // set access flags of the new file to the default flags
            CmsFile newfile = cms.readFile(newFolder, file.getName());
            Hashtable startSettings = null;
            Integer accessFlags = null;
            startSettings = (Hashtable)cms.getRequestContext().currentUser().getAdditionalInfo(C_ADDITIONAL_INFO_STARTSETTINGS);
            if(startSettings != null) {
                accessFlags = (Integer)startSettings.get(C_START_ACCESSFLAGS);
            }
            if(accessFlags == null) {
                accessFlags = new Integer(C_ACCESS_DEFAULT_FLAGS);
            }
            newfile.setAccessFlags(accessFlags.intValue());
            cms.writeFile(newfile);
        }
    }
    
    /**
     * Renames a file.
     * If the file is a page file, its content will be deleted, too.
     * @param cms The CmsObject.
     * @param file The file to be renamed.
     * @param newFile The new name of the file.
     * @exception Throws CmsException if something goes wrong.
     */
    
    private void renameFile(CmsObject cms, CmsResource file, String newFile) throws CmsException {
        if((cms.getResourceType(file.getType()).getResourceName()).equals(C_TYPE_PAGE_NAME)) {
            String bodyPath = getBodyPath(cms, (CmsFile)file);
            int help = C_CONTENTBODYPATH.lastIndexOf("/");
            String hbodyPath = (C_CONTENTBODYPATH.substring(0, help)) + (file.getAbsolutePath());
            if(hbodyPath.equals(bodyPath)) {
                cms.renameFile(bodyPath, newFile);
                help = bodyPath.lastIndexOf("/") + 1;
                hbodyPath = bodyPath.substring(0, help) + newFile;
                changeContent(cms, (CmsFile)file, hbodyPath);
            }
        }
        cms.renameFile(file.getAbsolutePath(), newFile);
    }
    
    /**
     * Pre-Sets the value of the new name input field.
     * This method is directly called by the content definiton.
     * @param Cms The CmsObject.
     * @param lang The language file.
     * @param parameters User parameters.
     * @return Value that is pre-set into the anew name field.
     * @exception CmsExeption if something goes wrong.
     */
    
    public String setValue(CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) throws CmsException {
        I_CmsSession session = cms.getRequestContext().getSession(true);
        String name = (String)session.getValue("name");
        return name;
    }
}
