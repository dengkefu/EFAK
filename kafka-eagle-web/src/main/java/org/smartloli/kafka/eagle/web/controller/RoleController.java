/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartloli.kafka.eagle.web.controller;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.smartloli.kafka.eagle.api.email.MailFactory;
import org.smartloli.kafka.eagle.api.email.MailService;
import org.smartloli.kafka.eagle.common.util.GzipUtils;
import org.smartloli.kafka.eagle.web.pojo.RoleResource;
import org.smartloli.kafka.eagle.web.pojo.Signiner;
import org.smartloli.kafka.eagle.web.pojo.UserRole;
import org.smartloli.kafka.eagle.web.service.AccountService;
import org.smartloli.kafka.eagle.web.service.RoleService;
import org.smartloli.kafka.eagle.web.sso.filter.SSORealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Sets the user roles and distributes the access resource directories to each
 * role.
 * 
 * @author smartloli.
 *
 *         Created by May 26, 2017.
 */
@Controller
@RequestMapping("/system")
public class RoleController {

	@Autowired
	private RoleService roleService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private SSORealm ssoRealm;

	/** Role viewer. */
	@RequiresPermissions("/system/role")
	@RequestMapping(value = "/role", method = RequestMethod.GET)
	public ModelAndView roleView(HttpSession session) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("/system/role");
		return mav;
	}

	/** User viewer. */
	@RequiresPermissions("/system/user")
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public ModelAndView userView(HttpSession session) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("/system/user");
		return mav;
	}

	/** Notice viewer. */
	@RequiresPermissions("/system/notice")
	@RequestMapping(value = "/notice", method = RequestMethod.GET)
	public ModelAndView noticeView(HttpSession session) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("/system/notice");
		return mav;
	}

	/** Add user. */
	@RequestMapping(value = "/user/add/", method = RequestMethod.POST)
	public String addUser(HttpSession session, HttpServletRequest request) {
		String rtxno = request.getParameter("ke_rtxno_name");
		String username = request.getParameter("ke_user_name");
		String realname = request.getParameter("ke_real_name");
		String email = request.getParameter("ke_user_email");

		Signiner signin = new Signiner();
		signin.setEmail(email);
		signin.setPassword(UUID.randomUUID().toString().substring(0, 8));
		signin.setRealname(realname);
		signin.setRtxno(Integer.parseInt(rtxno));
		signin.setUsername(username);
		if (accountService.insertUser(signin) > 0) {
			MailService mail = new MailFactory().create();
			String content = "You can use account(" + signin.getUsername() + ") or rtxno(" + signin.getRtxno() + ") signin, and you password is : [" + signin.getPassword()
					+ "], you can change the password in the system personal settings. Hope you have a nice day.";
			mail.send("*** Password ***", signin.getEmail(), content, null);
			return "redirect:/system/user";
		} else {
			return "redirect:/errors/500";
		}
	}

	/** Get the roles that the user owns. */
	@RequestMapping(value = "/user/role/table/ajax", method = RequestMethod.GET)
	public void getUserRoleAjax(HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		String aoData = request.getParameter("aoData");
		JSONArray params = JSON.parseArray(aoData);
		int sEcho = 0, iDisplayStart = 0, iDisplayLength = 0;
		String search = "";
		for (Object object : params) {
			JSONObject param = (JSONObject) object;
			if ("sEcho".equals(param.getString("name"))) {
				sEcho = param.getIntValue("value");
			} else if ("iDisplayStart".equals(param.getString("name"))) {
				iDisplayStart = param.getIntValue("value");
			} else if ("iDisplayLength".equals(param.getString("name"))) {
				iDisplayLength = param.getIntValue("value");
			} else if ("sSearch".equals(param.getString("name"))) {
				search = param.getString("value");
			}
		}

		Map<String, Object> map = new HashMap<>();
		map.put("search", search);
		map.put("start", iDisplayStart);
		map.put("size", iDisplayLength);

		JSONArray roles = JSON.parseArray(accountService.findUserBySearch(map).toString());
		JSONArray aaDatas = new JSONArray();
		for (Object object : roles) {
			JSONObject role = (JSONObject) object;
			JSONObject obj = new JSONObject();
			obj.put("rtxno", role.getString("rtxno"));
			obj.put("username", role.getString("username"));
			obj.put("realname", role.getString("realname"));
			obj.put("email", role.getString("email"));
			obj.put("operate", "<a id='operater_modal' name='operater_modal' href='#" + role.getInteger("id") + "' class='btn btn-primary btn-xs'>分配角色</a>");
			aaDatas.add(obj);
		}

		int count = accountService.userCounts();
		JSONObject target = new JSONObject();
		target.put("sEcho", sEcho);
		target.put("iTotalRecords", count);
		target.put("iTotalDisplayRecords", count);
		target.put("aaData", aaDatas);
		try {
			byte[] output = GzipUtils.compressToByte(target.toJSONString());
			response.setContentLength(output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Get all the roles of the system. */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/role/table/ajax", method = RequestMethod.GET)
	public void getRolesAjax(HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		String aoData = request.getParameter("aoData");
		JSONArray params = JSON.parseArray(aoData);
		int sEcho = 0, iDisplayStart = 0, iDisplayLength = 0;
		String search = "";
		for (Object object : params) {
			JSONObject param = (JSONObject) object;
			if ("sEcho".equals(param.getString("name"))) {
				sEcho = param.getIntValue("value");
			} else if ("iDisplayStart".equals(param.getString("name"))) {
				iDisplayStart = param.getIntValue("value");
			} else if ("iDisplayLength".equals(param.getString("name"))) {
				iDisplayLength = param.getIntValue("value");
			} else if ("sSearch".equals(param.getString("name"))) {
				search = param.getString("value");
			}
		}

		JSONArray roles = JSON.parseArray(roleService.getRoles().toString());
		JSONArray aaDatas = new JSONArray();
		for (Object object : roles) {
			JSONObject role = (JSONObject) object;
			JSONObject obj = new JSONObject();
			obj.put("name", role.getString("roleName"));
			obj.put("describer", role.getString("roleDescriber"));
			obj.put("operate", "<a id='operater_modal' name='operater_modal' href='#" + role.getInteger("id") + "' class='btn btn-primary btn-xs'>Setting</a>");
			aaDatas.add(obj);
		}

		int count = roles.size();
		JSONObject target = new JSONObject();
		target.put("sEcho", sEcho);
		target.put("iTotalRecords", count);
		target.put("iTotalDisplayRecords", count);
		target.put("aaData", aaDatas);
		try {
			byte[] output = GzipUtils.compressToByte(target.toJSONString());
			response.setContentLength(output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Obtain the resources it owns through the role id. */
	@RequestMapping(value = "/role/resource/{roleId}/ajax", method = RequestMethod.GET)
	public void roleResourceAjax(@PathVariable("roleId") int roleId, HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		try {
			byte[] output = GzipUtils.compressToByte(roleService.getRoleTree(roleId));
			response.setContentLength(output == null ? "NULL".toCharArray().length : output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Change the user's role. */
	@RequestMapping(value = "/user/role/{action}/{userId}/{roleId}/ajax", method = RequestMethod.GET)
	public void changeUserRoleAjax(@PathVariable("action") String action, @PathVariable("userId") int userId, @PathVariable("roleId") int roleId, HttpServletResponse response,
			HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		try {
			UserRole userRole = new UserRole();
			userRole.setUserId(userId);
			userRole.setRoleId(roleId);
			JSONObject object = new JSONObject();
			int code = 0;
			if ("add".equals(action)) {
				code = roleService.insertUserRole(userRole);
				if (code > 0) {
					object.put("info", "Add role has successed.");
				} else {
					object.put("info", "Add role has failed.");
				}
			} else if ("delete".equals(action)) {
				code = roleService.deleteUserRole(userRole);
				if (code > 0) {
					object.put("info", "Delete role has successed.");
				} else {
					object.put("info", "Delete role has failed.");
				}
			}
			object.put("code", code);
			byte[] output = GzipUtils.compressToByte(object.toJSONString());
			response.setContentLength(output == null ? "NULL".toCharArray().length : output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Get the corresponding roles through the user id. */
	@RequestMapping(value = "/user/role/{userId}/ajax", method = RequestMethod.GET)
	public void userRoleAjax(@PathVariable("userId") int userId, HttpServletResponse response, HttpServletRequest request) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		try {
			JSONObject object = new JSONObject();
			object.put("role", roleService.getRoles());
			object.put("userRole", roleService.findRoleByUserId(userId));
			byte[] output = GzipUtils.compressToByte(object.toJSONString());
			response.setContentLength(output == null ? "NULL".toCharArray().length : output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** Change the resources that you have by role id. */
	@RequestMapping(value = "/role/{action}/{roleId}/{resourceId}/", method = RequestMethod.GET)
	public void changeRoleResource(@PathVariable("action") String action, @PathVariable("roleId") int roleId, @PathVariable("resourceId") int resourceId,
			HttpServletResponse response) {
		response.setContentType("text/html;charset=utf-8");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Charset", "utf-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Encoding", "gzip");

		try {
			JSONObject object = new JSONObject();
			RoleResource roleResource = new RoleResource();
			roleResource.setRoleId(roleId);
			roleResource.setResourceId(resourceId);
			if ("insert".equals(action)) {
				int code = roleService.insertRoleResource(roleResource);
				object.put("code", code);
				if (code > 0) {
					ssoRealm.clearAllCached();
					object.put("info", "Add role has successed.");
				} else {
					object.put("info", "Add role has failed.");
				}
			} else if ("delete".equals(action)) {
				int code = roleService.deleteRoleResource(roleResource);
				object.put("code", code);
				if (code > 0) {
					ssoRealm.clearAllCached();
					object.put("info", "Delete role has successed.");
				} else {
					object.put("info", "Delete role has failed.");
				}
			}
			byte[] output = GzipUtils.compressToByte(object.toJSONString());
			response.setContentLength(output == null ? "NULL".toCharArray().length : output.length);
			OutputStream out = response.getOutputStream();
			out.write(output);

			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}