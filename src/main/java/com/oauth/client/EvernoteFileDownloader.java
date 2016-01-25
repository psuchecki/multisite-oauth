package com.oauth.client;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.github.scribejava.core.model.Token;
import com.google.common.collect.Lists;
import com.oauth.PropertiesHolder;
import com.oauth.handler.EvernoteOAuthHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.oauth.handler.EvernoteOAuthHandler.APP_NAME;
import static com.oauth.handler.EvernoteOAuthHandler.EVERNOTE_API;

public class EvernoteFileDownloader implements FileDownloaderApi {

    public static final EvernoteService EVERNOTE_SERVICE =
            PropertiesHolder.useEvernoteSandbox() ? EvernoteService.SANDBOX : EvernoteService.PRODUCTION;

    @Override
    public Token downloadUserFiles(String rawResponse) throws Exception {
        Token accessToken = EVERNOTE_API.getAccessTokenExtractor().extract(rawResponse);
        List<Note> userNotes = Lists.newArrayList();
        EvernoteAuth evernoteAuth = new EvernoteAuth(EVERNOTE_SERVICE, accessToken.getToken());
        NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();

        for (Notebook notebook : noteStoreClient.listNotebooks()) {
            NoteFilter filter = new NoteFilter();
            filter.setNotebookGuid(notebook.getGuid());
            filter.setOrder(NoteSortOrder.CREATED.getValue());
            filter.setAscending(true);
            NoteList noteList = noteStoreClient.findNotes(filter, 0, Integer.MAX_VALUE);
            List<Note> notes = noteList.getNotes();

            Path path = Paths.get(String.format("target/%s/%s.txt", EvernoteOAuthHandler.APP_NAME, notebook.getGuid()));
            Files.createDirectories(path.getParent());
            Files.deleteIfExists(path);
            Files.createFile(path);

            for (Note note : notes) {
                Note noteWithContent = noteStoreClient.getNote(note.getGuid(), true, true, false, false);
                List<Resource> resources = noteWithContent.getResources();
                if (resources != null) {
                    for (Resource resource : resources) {
                        byte[] fileContent = resource.getData().getBody();
                        String fileName = resource.getAttributes().getFileName();
                        String filePath = String.format("target/%s/%s-%s", APP_NAME, note.getGuid(), fileName);
                        com.google.common.io.Files.write(fileContent, new File(filePath));
                    }
                }
                noteWithContent.setResources(Collections.emptyList());
                userNotes.add(noteWithContent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                for (Note userNote : userNotes) {
                    writer.write(userNote.getContent());
                }
            }
        }

        return accessToken;
    }
}
