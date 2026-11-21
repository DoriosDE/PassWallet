package org.ligi.passandroid;

import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.ligi.passandroid.model.InputStreamWithSource;
import org.ligi.passandroid.model.PassStore;
import org.ligi.passandroid.ui.PassImportController;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.assertj.core.api.Fail.fail;
import static org.ligi.passandroid.ui.PassImportController.InputStreamImportSpec;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ThePassImportController  {

    @Mock
    PassImportController.FailCallback failCallback;

    @Mock
    PassImportController.SuccessCallback successCallback;

    @Mock
    PassStore passStore;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testShouldFailForBrokenPass() {

        try {

            final InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getResources().getAssets().open("passes/broken/fail.pkpass");
            final InputStreamWithSource inputStreamWithSource = new InputStreamWithSource("none", inputStream);
            final InputStreamImportSpec spec = new InputStreamImportSpec(inputStreamWithSource,
                                                                                           InstrumentationRegistry.getInstrumentation().getTargetContext(),
                                                                                           passStore,
                                                                                           successCallback,
                                                                                           failCallback);
            PassImportController.INSTANCE.processInputStream(spec);

            verify(successCallback, never()).call(anyList());
            verify(failCallback).fail(any(String.class));

        } catch (Exception e) {
            fail("should be able to load file " + e);
        }
    }

}
