package com.firti.webservice.services.impl;

import com.firti.webservice.commons.PageAttr;
import com.firti.webservice.commons.utils.NetworkUtil;
import com.firti.webservice.commons.utils.PasswordUtil;
import com.firti.webservice.commons.utils.SessionIdentifierGenerator;
import com.firti.webservice.config.security.SecurityConfig;
import com.firti.webservice.entities.AcValidationToken;
import com.firti.webservice.entities.Role;
import com.firti.webservice.entities.User;
import com.firti.webservice.exceptions.exists.UserAlreadyExistsException;
import com.firti.webservice.exceptions.forbidden.ForbiddenException;
import com.firti.webservice.exceptions.invalid.InvalidException;
import com.firti.webservice.exceptions.invalid.UserInvalidException;
import com.firti.webservice.exceptions.notfound.UserNotFoundException;
import com.firti.webservice.exceptions.nullpointer.NullPasswordException;
import com.firti.webservice.exceptions.unknown.UnknownException;
import com.firti.webservice.repositories.UserRepository;
import com.firti.webservice.services.AcValidationTokenService;
import com.firti.webservice.services.MailService;
import com.firti.webservice.services.RoleService;
import com.firti.webservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepo;
    private final AcValidationTokenService acValidationTokenService;
    private final MailService mailService;
    private final RoleService roleService;
    private final RegistrationAttemptService registrationAttemptService;

    @Value("${applicationName}")
    private String applicationName;
    @Value("${baseUrlApi}")
    private String baseUrlApi;
    @Value(("${admin.phone1}"))
    private String adminPhone1;
    @Value(("${admin.phone2}"))
    private String adminPhone2;

    @Autowired
    public UserServiceImpl(UserRepository userRepo, AcValidationTokenService acValidationTokenService, MailService mailService, RoleService roleService, RegistrationAttemptService registrationAttemptService) {
        this.userRepo = userRepo;
        this.acValidationTokenService = acValidationTokenService;
        this.mailService = mailService;
        this.roleService = roleService;
        this.registrationAttemptService = registrationAttemptService;
    }

    @Override
    public User findByUsername(String username) throws UserNotFoundException {
        if (username == null) throw new UserNotFoundException("Username can not be null!");
        return this.userRepo.findByUsername(username);
    }

    @Override
    public User findByPhoneNumber(String phoneNumber) throws UserNotFoundException {
        if (phoneNumber == null) throw new UserNotFoundException("Phone number can not be null!");
        return this.userRepo.findByPhoneNumber(phoneNumber);
    }

    @Override
    public User findByEmail(String email) throws UserNotFoundException {
        if (email == null) throw new UserNotFoundException("Email can not be null!");
        return this.userRepo.findByPhoneNumber(email);
    }

    @Override
    public User findByUsernameOrPhone(String usernameOrPhone) throws UserNotFoundException {
        User user = this.userRepo.findByUsername(usernameOrPhone);
        if (user == null)
            user = this.userRepo.findByPhoneNumber(usernameOrPhone);
        if (user == null)
            throw new UserNotFoundException("Could not find user with username or email " + usernameOrPhone);
        return user;
    }

    @Override
    public Page<User> searchUser(String query, int page, int size) {
        return this.userRepo.searchByNameOrUsername(query, PageAttr.getPageRequest(page, size));
    }

    @Override
    public Page<User> findAll(int page) {
        if (page < 0) page = 0;
        return this.userRepo.findAll(PageAttr.getPageRequest(page));
    }

    @Override
    public Page<User> findByRole(String role, int page) {
        return this.userRepo.findByRolesName(role, PageAttr.getPageRequest(page));
    }

    @Override
    public List<User> findByRole(String role) {
        return this.userRepo.findByRolesName(role);
    }

    @Override
    public User findOne(Long id) throws UserNotFoundException {
        if (id == null) throw new UserNotFoundException("User id can not be null!");
        return this.userRepo.findOne(id);
    }

    @Override
    public User save(User user) throws UserAlreadyExistsException, UserInvalidException, NullPasswordException {
        if (!this.isValid(user)) throw new UserInvalidException("User invalid");

        // check if user already exists
        if (user.getId() == null && this.exists(user))
            throw new UserAlreadyExistsException("User already exists with this email or username");
        if (user.getPhoneNumber() == null)
            throw new UserInvalidException("Password length must be at least 6 or more!");
        if (user.getPassword() == null || user.getPassword().length() < 6)
            throw new UserInvalidException("Password length must be at least 6 or more!");

        // set Roles
        user.grantRole(this.roleService.findRole(Role.ERole.ROLE_USER));

        // Execute only when user is being registered
        if (user.getId() == null) {
            // Encrypt passwprd
            user.setPassword(PasswordUtil.encryptPassword(user.getPassword(), PasswordUtil.EncType.BCRYPT_ENCODER, null));
            if (user.getPhoneNumber().equals(this.adminPhone1) || user.getPhoneNumber().equals(this.adminPhone2))
                user.grantRole(this.roleService.findRole(Role.ERole.ROLE_ADMIN));

            // flood control
            String ip = NetworkUtil.getClientIP();
            if (this.registrationAttemptService.isBlocked(ip))
                throw new UserInvalidException("Maximum limit exceed!");
            this.registrationAttemptService.registrationSuccess(ip);
        }
//        boolean newUser = user.getId()==null;
//        user = this.userRepo.save(user);
//        if (newUser) try {
//            if (!user.isOnlyUser())
//                this.requireAccountValidationByEmail(user.getEmail(), "/register/verify");
//        } catch (UserNotFoundException e) {
//            e.printStackTrace();
//        }
        return this.userRepo.save(user);
    }

    private boolean isValid(User user) {
        return user != null && user.getPassword() != null;
    }

    @Override
    public boolean exists(User user) {
        if (user == null) throw new IllegalArgumentException("user can't be null");
        return this.userRepo.findByUsername(user.getUsername()) != null
                || this.userRepo.findByPhoneNumber(user.getPhoneNumber()) != null;
    }

    @Override
    public User getAuthentication(String username, String password) throws UserNotFoundException, NullPasswordException {
        User user = this.findByUsernameOrPhone(username);
        if (PasswordUtil.matches(user.getPassword(), password))
            return user;
        return null;
    }

    @Override
    public void requireAccountValidationByEmail(String email, String validationUrl) throws UserNotFoundException {
        if (email == null) throw new IllegalArgumentException("Email invalid!");
        User user = this.findByEmail(email);
        SessionIdentifierGenerator sessionIdentifierGenerator = new SessionIdentifierGenerator();
        AcValidationToken acValidationToken = new AcValidationToken();
        acValidationToken.setToken(sessionIdentifierGenerator.nextSessionId());
        acValidationToken.setTokenValid(true);
        acValidationToken.setUser(user);
        // save acvalidationtoken
        acValidationToken = this.acValidationTokenService.save(acValidationToken);
        if (validationUrl == null) {
            this.mailService.sendEmail(user.getEmail(), "ShareMyRevenue verification token", "Your verification token is: " + acValidationToken.getToken());
            return;
        }
        // build confirmation link
        String confirmationLink = baseUrlApi.trim() + validationUrl + "?token=" + acValidationToken.getToken() + "&enabled=true";
        // send link by email
        this.mailService.sendEmail(user.getEmail(), "Please verify you ShareMyRevenue account", "Please verify your email by clicking this link " + confirmationLink);
    }

    @Override
    @Transactional
    public User resetPassword(String username, String token, String password) throws NullPasswordException, UserAlreadyExistsException, UserInvalidException, ForbiddenException {
        if (password.length() < 6)
            throw new ForbiddenException("Password length should be at least 6");
        AcValidationToken acValidationToken = this.acValidationTokenService.findByToken(token);
        User user = acValidationToken.getUser();
        if (username == null || !username.equals(user.getUsername()))
            throw new ForbiddenException("You are not authorized to do this action!");
        user.setPassword(PasswordUtil.encryptPassword(password, PasswordUtil.EncType.BCRYPT_ENCODER, null));
        acValidationToken.setTokenValid(false);
        acValidationToken.setReason("Password Reset");
        user = this.save(user);
        acValidationToken.setUser(user);
        this.acValidationTokenService.save(acValidationToken);
        return user;
    }

    @Override
    public Page<User> findUsersIn(List<Long> userIds, int page) {
        return this.userRepo.findByIdIn(userIds, new PageRequest(page, PageAttr.PAGE_SIZE, Sort.Direction.DESC, PageAttr.SORT_BY_FIELD_ID));
    }

    @Override
    public User changeRole(Long id, String role) throws UserNotFoundException {
        User user = this.findOne(id);
        if (user == null) throw new UserNotFoundException("Could not find user with id " + id);
        Role r = this.roleService.findRole(Role.getERole(role));
        user.changeRole(r);
        user = this.userRepo.save(user);
        SecurityConfig.updateAuthentication(user);
        return user;
    }

    @Override
    public User changePassword(Long id, String currentPassword, String newPassword) throws NullPasswordException, UserNotFoundException, InvalidException, ForbiddenException {
        User user = this.findOne(id);
        if (user == null) throw new UserNotFoundException("Could not find user with id " + id);

        if (!PasswordUtil.matches(user.getPassword(), currentPassword))
            throw new ForbiddenException("Password doesn't match");

        if (newPassword.length() < 6) throw new InvalidException("Password invalid");
        user.setPassword(PasswordUtil.encryptPassword(newPassword, PasswordUtil.EncType.BCRYPT_ENCODER, null));
        user = this.userRepo.save(user);
        return user;
    }

    @Override
    public User setPassword(Long id, String newPassword) throws NullPasswordException, UserNotFoundException, InvalidException, ForbiddenException {
        User currentUser = SecurityConfig.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin())
            throw new ForbiddenException("You are not authorised to do this action.");

        User user = this.findOne(id);
        if (user == null) throw new UserNotFoundException("Could not find user with id " + id);

        if (newPassword.length() < 6) throw new InvalidException("Password invalid");
        user.setPassword(PasswordUtil.encryptPassword(newPassword, PasswordUtil.EncType.BCRYPT_ENCODER, null));
        user = this.userRepo.save(user);
        return user;
    }

    @Override
    public void handlePasswordResetRequest(String username) throws UserNotFoundException, ForbiddenException, UnknownException {
        User user = this.findByUsername(username);
        if (this.acValidationTokenService.isLimitExceeded(user))
            throw new ForbiddenException("Limit exceeded!");

        int otp = SessionIdentifierGenerator.generateOTP();
        String message = "Your " + this.applicationName + " OTP is: " + otp;
        boolean success = NetworkUtil.sendSms(user.getUsername(), message);
        // save validation token
        if (!success) throw new UnknownException("Could not send SMS");

        AcValidationToken resetToken = new AcValidationToken();
        resetToken.setUser(user);
        resetToken.setToken(String.valueOf(otp));
        resetToken.setTokenValid(true);
        this.acValidationTokenService.save(resetToken);
    }

    @Override
    public User setRoles(Long id, String[] roleNames) throws UserNotFoundException, UserAlreadyExistsException, NullPasswordException, UserInvalidException {
        User user = this.findOne(id);
        boolean isAdmin = user.isAdmin(); // check if user admin
        user.getRoles().clear();
        if (isAdmin)  // set admin role explicitly after clearing roles
            user.getRoles().add(this.roleService.findRole(Role.ERole.ROLE_ADMIN));
        // add roles
        for (String roleName : roleNames) {
            Role role = this.roleService.findRole(Role.getERoleFromRoleName(roleName));
            if (role == null) continue;
            user.grantRole(role);
        }
        return this.save(user);
    }


}
