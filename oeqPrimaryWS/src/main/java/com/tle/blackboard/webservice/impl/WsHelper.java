package com.tle.blackboard.webservice.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import com.tle.blackboard.webservice.Course;
import com.tle.blackboard.webservice.Folder;

import blackboard.data.content.Content;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.platform.security.Entitlement;
import blackboard.platform.security.SecurityUtil;
import blackboard.platform.ws.AxisHelpers;

public class WsHelper {
	private static final boolean DEV = false;

	public static void ensurePermission(BbWsSession session, Id courseId, String permission) {
		if (!SecurityUtil.userHasEntitlement(session.getUserId(), courseId, new Entitlement(permission))) {
			chuckIt(new RuntimeException(
					"User does not have " + permission + " entitlement for course " + courseId.toExternalString()),
					"EQ100");
		}
	}

	public static void chuckIt(Throwable t, String code) {
		WebServiceUtil.error("Error occurred", t);
		String message = t.getMessage();
		if (DEV) {
			StringWriter sw = new StringWriter();
			PrintWriter w = new PrintWriter(sw);
			t.printStackTrace(w);
			message += "<pre>" + sw.toString() + "</pre>";
		}
		AxisHelpers.throwWSException(code, message);
	}

	public static Folder getFolder(BbWsSession session, Map<String, Folder> folderMap, Course course, String folderId)
			throws PersistenceException {
		Folder folder = folderMap.get(folderId);
		if (folder == null) {
			// WebServiceUtil.debug("Folder " + folderId +
			// " not found in map, loading it");
			Content bbFolder = session.loadContent(folderId);
			if (bbFolder != null) {
				folder = WebServiceUtil.convertFolder(bbFolder, course.getId());
				folderMap.put(folderId, folder);
			} else {
				WebServiceUtil.debug("Folder could not be loaded.  It doesn't exist any more!");
			}
		}
		return folder;
	}

	public static boolean canCreate(Set<Id> coursesWithCreate, Id courseId) {
		if (coursesWithCreate == null) {
			return true;
		}
		for (Id id : coursesWithCreate) {
			if (id.toExternalString().equals(courseId.toExternalString())) {
				return true;
			}
		}
		return false;
	}
}