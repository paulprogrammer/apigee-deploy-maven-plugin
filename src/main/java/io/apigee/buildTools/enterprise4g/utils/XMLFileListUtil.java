/**
 * Copyright (C) 2014 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apigee.buildTools.enterprise4g.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLFileListUtil {

	public List<File> getProxyFiles(File configFile) throws IOException { // assumes the present directory is at
		// the project pom level.

		List<File> fileNames = new ArrayList<File>();
		String sDirectory = configFile.getParent()+File.separator+"target" + File.separator + "apiproxy" + File.separator + "proxies";

		fileNames = getXMLFiles(sDirectory);

		return fileNames;

	}

	public List<File> getPolicyFiles(File configFile) throws IOException { // assumes the present directory is at
		// the project pom level.

		List<File> fileNames = new ArrayList<File>();

		String sDirectory = configFile.getParent()+File.separator+"target" + File.separator + "apiproxy" + File.separator + "policies";

		fileNames = getXMLFiles(sDirectory);

		List<File> stepNames = getStepFiles(configFile);
		if(stepNames != null) {
			fileNames.addAll(stepNames);
		}

		return fileNames;

	}

	private List<File> getStepFiles(File configFile) throws IOException { // assumes the present directory is at
		// the project pom level.

		List<File> fileNames = new ArrayList<File>();

		String sDirectory = configFile.getParent()+File.separator+"target" + File.separator + "apiproxy" + File.separator + "stepdefinitions";

		fileNames = getXMLFiles(sDirectory);

		return fileNames;

	}

	public List<File> getTargetFiles(File configFile) throws IOException { // assumes the present directory is at
		// the project pom level.

		List<File> fileNames = new ArrayList<File>();

		String sDirectory = configFile.getParent()+File.separator+"target" + File.separator + "apiproxy" + File.separator + "targets";

		fileNames = getXMLFiles(sDirectory);

		return fileNames;

	}
	
	public List<File> getAPIProxyFiles(File configFile) throws IOException { // assumes the present directory is at
		// the project pom level.

		List<File> fileNames = new ArrayList<File>();

		String sDirectory = configFile.getParent()+File.separator+"target" + File.separator + "apiproxy";

		fileNames = getXMLFiles(sDirectory);

		return fileNames;

	}

	private List<File> getXMLFiles(String sFolder) { // assumes the present
		// directory is at the
		// project pom level.

		ArrayList<File> aList = new ArrayList<File>();
		Logger logger = LoggerFactory.getLogger(XMLFileListUtil.class);
		try {
			File folder = new File(sFolder);

			ExtFileNameFilter xmlFilter = new ExtFileNameFilter("xml");

			logger.debug("=============Searching for XML files in the following directory ================\n{}", folder.getAbsolutePath());

			aList = new ArrayList<File>(Arrays.asList(folder.listFiles(xmlFilter)));

			logger.debug("=============Nuber of files found is================\n{}", aList.size());
		} catch (Exception e) {
			logger.debug("=============Error Encountered in Searching files [" + sFolder + "]================\n" + e);
		}

		return aList;

	}

}
