package latest;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.core.util.StringUtils;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.util.GroupToPermissionSchemeMapper;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.GlobalPermissionGroupAssociationUtil;
import com.atlassian.jira.web.SessionKeys;
import com.atlassian.jira.web.action.AbstractBrowser;
import com.atlassian.jira.web.bean.GroupBrowserFilter;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.Group;
import com.opensymphony.user.ImmutableException;
import com.opensymphony.util.TextUtils;
import org.ofbiz.core.entity.GenericEntityException;
import webwork.action.ActionContext;
import webwork.util.BeanUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author dmitry dementiev
 */
public class MyGroupBrowser extends AbstractBrowser {
    private List groups;
    private String addName;
    private String company;
    private GroupToPermissionSchemeMapper groupPermissionSchemeMapper;
    private ApplicationProperties applicationProperties;
    private final GlobalPermissionGroupAssociationUtil globalPermissionGroupAssociationUtil;

    public MyGroupBrowser(GroupToPermissionSchemeMapper groupToPermissionSchemeMapper, ApplicationProperties applicationProperties, GlobalPermissionGroupAssociationUtil globalPermissionGroupAssociationUtil) {
        this.applicationProperties = applicationProperties;
        this.globalPermissionGroupAssociationUtil = globalPermissionGroupAssociationUtil;
        if (groupToPermissionSchemeMapper != null) {
            this.groupPermissionSchemeMapper = groupToPermissionSchemeMapper;
        } else {
            addErrorMessage(getText("groupbrowser.error.retrieve.group"));
        }
    }

    public MyGroupBrowser() throws GenericEntityException {
        this(new GroupToPermissionSchemeMapper(
                ManagerFactory.getPermissionSchemeManager(),
                ComponentManager.getInstance().getSchemePermissions()),
                ComponentManager.getInstance().getApplicationProperties(),
                ComponentManager.getComponentInstanceOfType(GlobalPermissionGroupAssociationUtil.class));
    }

    protected String doExecute() throws Exception {
        resetPager();

        BeanUtil.setProperties(params, getFilter());

        return SUCCESS;
    }

    @RequiresXsrfCheck
    public String doAdd() throws Exception {
        if (!addNewGroup()) {
            return ERROR;
        }
        return doExecute();
    }

    private boolean addNewGroup() throws Exception{
        //JRA-12112: If external *user* management is enabled, we do not allow the addtion of a new user.
        if (getApplicationProperties().getOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT)) {
            addErrorMessage(getText("admin.errors.cannot.add.groups.external.managment"));
            return false;
        }
        if (!GroupUtils.existsGroup(addName)) {   
            try {
//                if (TextUtils.stringSet(addName)) {
                    Group newGroup = GroupUtils.getGroupSafely(addName);
                    //add isCompany propertyset to our group
                    PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(newGroup.getName()));
                    if (request.getParameter("company") != null) {
                        propertySet.setBoolean("isCompany", true);
                    } else {
                        propertySet.setBoolean("isCompany", false);
                    }
               /* }else{
                    addError("addName", getText("groupbrowser.error.add", addName));
                }*/
                addName = null;
            }
            catch (ImmutableException e) {
                addError("addName", getText("groupbrowser.error.add", addName));
                log.error("Error occurred adding group : " + addName, e);
            }
        } else {
            addError("addName", getText("groupbrowser.error.group.exists"));
        }
        return true;
    }

    public PagerFilter getPager() {
        return getFilter();
    }

    public void resetPager() {
        ActionContext.getSession().put(SessionKeys.GROUP_FILTER, null);
    }

    public GroupBrowserFilter getFilter() {
        GroupBrowserFilter filter = (GroupBrowserFilter) ActionContext.getSession().get(SessionKeys.GROUP_FILTER);

        if (filter == null) {
            filter = new GroupBrowserFilter();
            ActionContext.getSession().put(SessionKeys.GROUP_FILTER, filter);
//            filter.setNameFilter();
        }

        return filter;
    }

    /**
     * Return the current 'page' of issues (given max and start) for the current filter
     */
    public List getCurrentPage() {
        return getFilter().getCurrentPage(getBrowsableItems());
    }

    public List getBrowsableItems() {
        if (groups == null) {
            try {
                groups = getFilter().getFilteredGroups();
            }
            catch (Exception e) {
                log.error("Exception getting groups: " + e, e);
                return Collections.EMPTY_LIST;
            }
        }

        return groups;
    }

    public String getAddName() {
        return addName;
    }

    public void setAddName(String addName) {
        this.addName = addName.trim();
    }

    public String escapeAmpersand(String str) {
        return StringUtils.replaceAll(str, "&", "%26");
    }

    public Collection getPermissionSchemes(String groupName) {
        if (groupPermissionSchemeMapper != null) {
            return groupPermissionSchemeMapper.getMappedValues(groupName);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public boolean isExternalUserManagementEnabled() {
        return applicationProperties.getOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT);
    }

    public boolean isUserAbleToDeleteGroup(String groupName) {
        return globalPermissionGroupAssociationUtil.isUserAbleToDeleteGroup(getRemoteUser(), groupName);
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        company = company;
    }
}
