package io.fd.maintainer.plugin;

import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountByEmailCache;
import com.google.gerrit.server.change.*;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MaintainerPluginModuleTest {

    @Mock
    @Bind
    private PluginConfigFactory pluginConfigFactory;

    @Mock
    @Bind
    private GitRepositoryManager repoManager;

    @Mock
    @Bind
    private SchemaFactory<ReviewDb> reviewDbSchemaFactory;

    @Mock
    @Bind
    private PatchListCache patchListCache;

    @Mock
    @Bind
    private ReviewDb reviewDb;

    @Mock
    @Bind
    private CurrentUser currentUser;

    @Mock
    @Bind
    private PostReview postReview;

    @Mock
    @Bind
    private PostReviewers postReviewers;

    @Mock
    @Bind
    private ChangesCollection changesCollection;

    @Mock
    @Bind
    private Revisions revisions;

    @Mock
    @Bind
    private Submit submitPusher;

    @Mock
    @Bind
    private AccountByEmailCache accountByEmailCache;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void configure() throws Exception {
        Guice.createInjector(new MaintainerPluginModule(), BoundFieldModule.of(this)).injectMembers(this);
    }

}