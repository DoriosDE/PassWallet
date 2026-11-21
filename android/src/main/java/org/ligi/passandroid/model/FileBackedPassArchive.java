package org.ligi.passandroid.model;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

public class FileBackedPassArchive extends PassArchive {

    private final JsonAdapter<Map> adapter;
    private final File backed_file;

    public FileBackedPassArchive(final File backed_file, final PassStore passStore, final Moshi moshi) {
        super(readStoredState(backed_file, getAdapter(moshi)), passStore);

        this.backed_file = backed_file;
        adapter = getAdapter(moshi);
    }

    private static JsonAdapter<Map> getAdapter(Moshi moshi) {
        return moshi.adapter(Map.class);
    }

    /**
     * Reads the stored state. A missing, unreadable or malformed file results in an empty state.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> readStoredState(final File backed_file, final JsonAdapter<Map> adapter) {
        if (backed_file.exists()) {
            try (BufferedSource source = Okio.buffer(Okio.source(backed_file))) {
                final Map<String, String> stored = (Map<String, String>) adapter.fromJson(source);
                if (stored != null) {
                    return stored;
                }
            } catch (IOException | RuntimeException e) {
                Timber.w(e, "could not read archive state");
            }
        }

        return new HashMap<>();
    }

    /**
     * Takes over the stored state of every pass mentioned in the file (e.g. after a backup was restored);
     * passes not mentioned keep their current state.
     */
    @Override
    public void mergeFromStorage() {
        getStateById().putAll(readStoredState(backed_file, adapter));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private BufferedSink getBufferedSinkFromMaybeCreatedFile() {
        try {
            if (!backed_file.exists()) {
                final File parentFile = backed_file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                backed_file.createNewFile();
            }

            return Okio.buffer(Okio.sink(backed_file));
        } catch (IOException e) {
            Timber.w(e, "could not open archive state for writing");
            return null;
        }
    }

    @Override
    protected void processDataChange() {
        super.processDataChange();

        // write
        if (adapter != null) {
            final BufferedSink buffer = getBufferedSinkFromMaybeCreatedFile();

            if (buffer != null) {
                try {
                    adapter.toJson(buffer, getStateById());
                    buffer.close();
                } catch (IOException e) {
                    Timber.w(e, "could not close archive state writer");
                }
            }
        }
    }
}
