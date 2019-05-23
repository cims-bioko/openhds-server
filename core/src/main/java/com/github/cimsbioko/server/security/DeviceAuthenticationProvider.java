package com.github.cimsbioko.server.security;

import com.github.cimsbioko.server.dao.DeviceRepository;
import com.github.cimsbioko.server.domain.Device;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;

public class DeviceAuthenticationProvider implements AuthenticationProvider {

    private final DeviceRepository repo;
    private final RoleMapper roleMapper;

    public DeviceAuthenticationProvider(DeviceRepository repo, RoleMapper roleMapper) {
        this.repo = repo;
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (!supports(authentication.getClass())) {
            return null;
        }

        DeviceAuthentication auth = (DeviceAuthentication) authentication;

        Device device = repo.findByToken(auth.getCredentials())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));

        return new DeviceAuthentication(device.getName(), device.getDescription(), roleMapper.rolesToAuthorities(device.getRoles()));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return DeviceAuthentication.class.isAssignableFrom(authentication);
    }
}
