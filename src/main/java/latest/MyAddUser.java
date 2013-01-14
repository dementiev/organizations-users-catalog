package latest;

import com.atlassian.core.user.GroupUtils;
import com.atlassian.jira.bc.user.UserService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.DuplicateEntityException;
import com.opensymphony.user.Group;
import com.opensymphony.user.ImmutableException;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import latest.util.I18n;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

/**
 * @author dmitry dementiev
 *         custom class for adding additional properties to User(company name(really group), telephone)
 */

public class MyAddUser extends JiraWebActionSupport implements I18n {
    private final static String BUNDLE_NAME = "dementievTextResource";
    private final I18nHelper i18Helper;
    private String username;
    private String password;
    private String confirm;
    private String fullname;
    private String email;
    private boolean sendEmail = false;
    //new fields
    private String companyName;
    private final UserService userService;
    private final UserUtil userUtil;
    private UserService.CreateUserValidationResult result;

    public MyAddUser(UserService userService, UserUtil userUtil, JiraAuthenticationContext authenticationContext) {
        this.userService = userService;
        this.userUtil = userUtil;
        i18Helper = authenticationContext.getI18nHelper(BUNDLE_NAME);
    }

    /**
     * Processes a request to render the input form to fill out the new user's details(username, password, full-name, email ...)
     *
     * @return {@link #INPUT} the input form to fill out the new user's details(username, password, full-name, email ...)
     */
    @Override
    public String doDefault() {
        //JRA-12112: Do not sent the e-mail if external management is enabled.
        sendEmail = !(getApplicationProperties().getOption(APKeys.JIRA_OPTION_USER_PASSWORD_EXTERNALMGT) || getApplicationProperties().getOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT));
        return INPUT;
    }

    protected void doValidation() {
        result = userService.validateCreateUserForAdmin(
                getRemoteUser(), getUsername(), getPassword(), getConfirm(), getEmail(), getFullname());
        if (!result.isValid()) {
//            addErrorCollection(result.getErrorCollection());
//            addErrorMessage("Please, fill correctly all the fields.");
            addErrorMessage(getText("user.error.fillcorectly"));
        }
    }

    /**
     * Processes a request to create a user using the specified url parameters.
     *
     * @return if there are input error this will return {@link #ERROR}; otherwise, it will redirect to the View User
     *         page for the created user.
     */
    @Override
    @RequiresXsrfCheck
    protected String doExecute() {
        try {
            User newUser = null;
            // send password if the user has not disabled when creating.
            if (sendEmail) {
                newUser = userService.createUser(result);
            } else {
                newUser = userService.createUserNoEvent(result);
            }
            //add user to group(company)
            if (TextUtils.stringSet(request.getParameter("checkbox_company"))) {
                if (TextUtils.stringSet(getCompanyName())) {
                    if (GroupUtils.existsGroup(getCompanyName())) {
                        Group group = GroupUtils.getGroup(getCompanyName());
                        if (group != null) {
                            userUtil.addUserToGroup(group, newUser);
                        }
                    }
                }
            }
        }
        catch (ImmutableException e) {
            throw new RuntimeException("cannot create user details", e);
        }
        catch (DuplicateEntityException e) {
            addError("username", getText("admin.errors.username.already.exists"));
        }

        if (getHasErrorMessages()) {
            return ERROR;
        } else {
            return getRedirect("ViewUser.jspa?name=" + JiraUrlCodec.encode(username.toLowerCase()));
        }
    }

    public boolean hasReachedUserLimit() {
        return !userUtil.canActivateNumberOfUsers(1);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.trim();
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (!TextUtils.stringSet(password)) {
            this.password = null;
        } else {
            this.password = password;
        }
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        if (!TextUtils.stringSet(confirm)) {
            this.confirm = null;
        } else {
            this.confirm = confirm;
        }
    }

    public UserUtil getUserUtil() {
        return userUtil;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * @return groups that have propertySet isCompany == 1
     */
    public Collection<Group> getCompanies() throws SQLException {
        Collection<Group> groups = GroupUtils.getGroups();
        //todo remove duplicates, stay only companies (isCompany==1)
        HashSet h = new HashSet(groups);
        groups.clear();
        groups.addAll(h);
        Iterator<Group> it = groups.iterator();
        while (it.hasNext()) {
            Group group = it.next();
            PropertySet propertySet = PropertySetUtil.getPropertySet("Group", PluginUtils.getGroupIdByName(group.getName()));
            if (!propertySet.getBoolean("isCompany")) {
                it.remove();
            }
        }
        return groups;
    }

    /**
     * This method returns Html Encoded Text from recource
     *
     * @param key
     * @return
     */
    public String getHtmlEncodedText(String key) {
        String txt = getText(key);
        if (TextUtils.stringSet(txt))
            return "";
        else
            return TextUtils.htmlEncode(txt).replace("'", "&#39;").replace(System.getProperty("line.separator"), "&#10;");
    }

    /**
     * @param key
     * @param locale
     * @return text from resource
     */
    public String getText(String key, Locale locale) {
        return i18Helper.getText(key, locale);
    }
}