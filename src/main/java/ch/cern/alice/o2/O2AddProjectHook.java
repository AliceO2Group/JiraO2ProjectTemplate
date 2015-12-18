package ch.cern.alice.o2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.blueprint.api.AddProjectHook;
import com.atlassian.jira.blueprint.api.ConfigureData;
import com.atlassian.jira.blueprint.api.ConfigureResponse;
import com.atlassian.jira.blueprint.api.ValidateData;
import com.atlassian.jira.blueprint.api.ValidateResponse;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.scheme.Scheme;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.workflow.WorkflowSchemeManager;

/**
 * 
 * @author Barthélémy von Haller
 * 
 */
public class O2AddProjectHook implements AddProjectHook {
	private static final Logger LOGGER = LogManager.getLogger(O2AddProjectHook.class);

	@Override
	public ValidateResponse validate(final ValidateData validateData) {
		return ValidateResponse.create();
	}

	private Long[] ConvertLongListToArray(List<Long> values) {
		Long[] result = new Long[values.size()];
		int i = 0;
		for (Long l : values)
			result[i++] = l;
		return result;
	}

	@Override
	public ConfigureResponse configure(final ConfigureData configureData) {
		Project project = configureData.project();

		// Set Project Category -> O2
		String categoryName = "O2";
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		ProjectCategory o2Category = projectManager.getProjectCategoryObjectByName(categoryName);
		if (o2Category != null) {
			projectManager.setProjectCategory(project, o2Category);
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" category. "
					+ "It is not set for the new project [%s]", categoryName, project.getName()));
		}

		// Worfklow scheme -> O2 simple worfklow scheme
		String workflowSchemeName = "O2 simple workflow scheme";
		WorkflowSchemeManager workflowSchemeManager = ComponentAccessor.getWorkflowSchemeManager();
		Scheme o2Scheme = workflowSchemeManager.getSchemeObject(workflowSchemeName);
		if (o2Scheme != null) {
			workflowSchemeManager.removeSchemesFromProject(project);
			workflowSchemeManager.addSchemeToProject(project, o2Scheme);
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", workflowSchemeName, project.getName()));
		}

		// Set Notification Scheme -> O2 Notification Scheme
		String notificationSchemeName = "O2 Notification Scheme";
		NotificationSchemeManager notificationSchemeManager = ComponentAccessor.getNotificationSchemeManager();
		Scheme notificationScheme = notificationSchemeManager.getSchemeObject(notificationSchemeName);
		if (notificationScheme != null) {
			notificationSchemeManager.removeSchemesFromProject(project);
			notificationSchemeManager.addSchemeToProject(project, notificationScheme);
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", notificationSchemeName, project.getName()));
		}

		// Set Permission Scheme -> O2 Permission Scheme
		String permissionSchemeName = "O2 Permission Scheme";
		PermissionSchemeManager permissionSchemeManager = ComponentAccessor.getPermissionSchemeManager();
		Scheme permissionScheme = permissionSchemeManager.getSchemeObject(permissionSchemeName);
		if (permissionScheme != null) {
			// Remove all defined permissions screen since there can be only ony
			// apparently
			permissionSchemeManager.removeSchemesFromProject(project);
			permissionSchemeManager.addSchemeToProject(project, permissionScheme);
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", permissionSchemeName, project.getName()));
		}

		// Set Screen Scheme
		String issueTypeScreenSchemeName = "O2 Issue Type Screen Scheme";
		IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = ComponentAccessor.getIssueTypeScreenSchemeManager();
		// Note: I couldn't find a way to get the Screen Scheme by String so I'm
		// iterating over all of them
		IssueTypeScreenScheme screenScheme = null;
		for (IssueTypeScreenScheme scheme : issueTypeScreenSchemeManager.getIssueTypeScreenSchemes()) {
			if (scheme.getName().equals(issueTypeScreenSchemeName)) {
				screenScheme = scheme;
				break;
			}
		}
		if (screenScheme != null) {
			issueTypeScreenSchemeManager.addSchemeAssociation(project, screenScheme);
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", issueTypeScreenSchemeName, project.getName()));
		}

		// Field Configuration Scheme
		String fieldConfigurationSchemeName = "O2 Field Configuration Scheme";
		FieldConfigSchemeManager fieldConfigSchemeManager = ComponentAccessor.getFieldConfigSchemeManager();
		FieldLayoutManager fieldLayoutManager = ComponentAccessor.getFieldLayoutManager();
		FieldLayoutScheme fieldLayoutScheme = null;
		for (FieldLayoutScheme scheme : fieldLayoutManager.getFieldLayoutSchemes()) {
			if (scheme.getName().equals(fieldConfigurationSchemeName)) {
				fieldLayoutScheme = scheme;
				break;
			}
		}
		if (fieldLayoutScheme != null) {
			fieldLayoutManager.addSchemeAssociation(project, fieldLayoutScheme.getId());
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", fieldConfigurationSchemeName, project.getName()));
		}

		// Issue Type Scheme
		String issueTypeSchemeName = "O2 Issue Type Scheme";
		IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
		// Note: I couldn't find a way to get the Field config Scheme by String
		// so I'm iterating over all of them
		FieldConfigScheme issueTypeScheme = null;
		for (FieldConfigScheme scheme : issueTypeSchemeManager.getAllSchemes()) {
			if (scheme.getName().equals(issueTypeSchemeName)) {
				issueTypeScheme = scheme;
				break;
			}
		}
		if (issueTypeScheme != null) {
			// First build the list of all project ids associated with the issue
			// type schem and add the new project id
			List<Long> projectIds = issueTypeScheme.getAssociatedProjectIds();
			List<Long> newListWithIds = new ArrayList<Long>();
			for (Long id : projectIds) {
				newListWithIds.add(id);
			}
			newListWithIds.add(project.getId());

			// Then update the field config scheme with this new list
			Long[] projectsContext = ConvertLongListToArray(newListWithIds);
			List<JiraContextNode> contexts = CustomFieldUtils.buildJiraIssueContexts(false, projectsContext,
					projectManager);
			FieldManager fieldManager = ComponentAccessor.getFieldManager();
			fieldConfigSchemeManager.updateFieldConfigScheme(issueTypeScheme, contexts,
					fieldManager.getConfigurableField(IssueFieldConstants.ISSUE_TYPE));
		} else {
			LOGGER.error(String.format("[O2 project template] Failed to find the \"%s\" scheme. "
					+ "It is not set for the new project [%s]", issueTypeSchemeName, project.getName()));
		}

		// Roles
		ProjectRoleService projectRoleService = ComponentAccessor.getComponentOfType(ProjectRoleService.class);
		SimpleErrorCollection errorCollection = new SimpleErrorCollection();
		projectRoleService.removeAllRoleActorsByProject(project, errorCollection);

		Collection<String> actorsAdmin = Arrays.asList("alice-jira-admins", "jira-administrators");
		Collection<String> actorsDev = Arrays.asList("alice-member");
		Collection<String> actorsUser = Arrays.asList("alice-member");

		ProjectRole projectRoleAdmin = projectRoleService.getProjectRoleByName("Administrators", errorCollection);
		ProjectRole projectRoleDev = projectRoleService.getProjectRoleByName("Developers", errorCollection);
		ProjectRole projectRoleUser = projectRoleService.getProjectRoleByName("Users", errorCollection);
		projectRoleService.addActorsToProjectRole(actorsAdmin, projectRoleAdmin, project, "atlassian-group-role-actor",
				errorCollection);
		projectRoleService.addActorsToProjectRole(actorsDev, projectRoleDev, project, "atlassian-group-role-actor",
				errorCollection);
		projectRoleService.addActorsToProjectRole(actorsUser, projectRoleUser, project, "atlassian-group-role-actor",
				errorCollection);

		if (errorCollection.hasAnyErrors()) {
			LOGGER.error(String.format("[O2 project template] Failed set roles to the new project [%s]",
					project.getName()));
			for (String message : errorCollection.getErrorMessages()) {
				System.out.println(message);
			}
		}

		projectManager.updateProject(project, project.getName(), project.getDescription(), project.getLeadUserKey(),
				project.getUrl(), AssigneeTypes.PROJECT_LEAD);

		return ConfigureResponse.create().setRedirect(
				"/plugins/servlet/project-config/" + project.getKey() + "/summary");
	}
}