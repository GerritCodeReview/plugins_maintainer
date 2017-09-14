package io.fd.maintainer.plugin.service;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.PluginConfigFactory;
import io.fd.maintainer.plugin.service.dto.PluginBranchSpecificSettings;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

public class SettingsProviderTest {

    @Mock
    private PluginConfigFactory cfg;

    private SettingsProvider provider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        /*
         * [branch "refs/heads/master"]
         pluginuser = vppmaintainerplugin
         maintainerfileref = master
         maintainerfile = MAINTAINER
         autoaddreviewers = true
         allowmaintainersubmit = false
         autosubmit = false
         dislikewarnings = false
         * */
        Config config = new Config();
        config.setString("branch", "refs/heads/master", "pluginuser", "vppmaintainerplugin");
        config.setString("branch", "refs/heads/master", "maintainerfileref", "master");
        config.setString("branch", "refs/heads/master", "maintainerfile", "MAINTAINER");
        config.setString("branch", "refs/heads/master", "autoaddreviewers", "true");
        config.setString("branch", "refs/heads/master", "allowmaintainersubmit", "false");
        config.setString("branch", "refs/heads/master", "autosubmit", "false");
        config.setString("branch", "refs/heads/master", "dislikewarnings", "false");

        when(cfg.getProjectPluginConfig(new Project.NameKey("vpp"), "maintainer")).thenReturn(config);
        provider = new SettingsProvider(cfg);
    }

    @Test
    public void getBranchSpecificSettings() throws Exception {
        PluginBranchSpecificSettings settings = provider.getBranchSpecificSettings("refs/for/stable/1707", new Project.NameKey("vpp"));
        assertFalse(settings.isAutoAddReviewers());
        assertFalse(settings.isAllowMaintainersSubmit());
        assertFalse(settings.isAutoSubmit());
        assertFalse(settings.isDislikeWarnings());
    }

}