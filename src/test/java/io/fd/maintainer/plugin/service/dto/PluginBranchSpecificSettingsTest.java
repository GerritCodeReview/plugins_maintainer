package io.fd.maintainer.plugin.service.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PluginBranchSpecificSettingsTest {

    @Test
    public void getFileRefMaster() throws Exception {
        final PluginBranchSpecificSettings pluginSettings = new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setBranch("refs/heads/*")
                .setFileRef("master")
                .createPluginSettings();
        assertEquals("refs/heads/master", pluginSettings.fullFileRef());
    }

    @Test
    public void getFileRefMasterSpecific() throws Exception {
        final PluginBranchSpecificSettings pluginSettings = new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setBranch("refs/heads/master")
                .setFileRef("master")
                .createPluginSettings();
        assertEquals("refs/heads/master", pluginSettings.fullFileRef());
    }

    @Test
    public void getFileRefStable() throws Exception {
        final PluginBranchSpecificSettings pluginSettings = new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setBranch("refs/heads/stable/*")
                .setFileRef("1707")
                .createPluginSettings();
        assertEquals("refs/heads/stable/1707", pluginSettings.fullFileRef());
    }

    @Test
    public void getFileRefNonRefsHeadsWildcardMaster() throws Exception {
        final PluginBranchSpecificSettings pluginSettings = new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setBranch("refs/*")
                .setFileRef("master")
                .createPluginSettings();
        assertEquals("refs/heads/master", pluginSettings.fullFileRef());
    }

    @Test
    public void getFileRefNonRefsHeadsWildcardStable() throws Exception {
        final PluginBranchSpecificSettings pluginSettings = new PluginBranchSpecificSettings.PluginSettingsBuilder()
                .setBranch("refs/*")
                .setFileRef("stable/1707")
                .createPluginSettings();
        assertEquals("refs/heads/stable/1707", pluginSettings.fullFileRef());
    }

}