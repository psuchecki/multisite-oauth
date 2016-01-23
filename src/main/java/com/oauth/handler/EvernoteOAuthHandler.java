package com.oauth.handler;

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
import com.github.scribejava.apis.EvernoteApi;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class EvernoteOAuthHandler implements OAuthHandler {
    private static final String VERIFIER_PARAM = "oauth_verifier";
    private static final String APP_NAME = "evernote";

    private OAuthService service;
    private Token requestToken;

    @Override
    public void registerServletHandler(ServletContextHandler contextHandler) {
        ServletContextRegister.registerServlets(contextHandler, new SigninServlet(), new CallbackServlet(), APP_NAME);
    }


    private class SigninServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            service = OAuthServiceProvider.getInstance(APP_NAME, EvernoteApi.Sandbox.class);
            requestToken = service.getRequestToken();
            String authorizationUrl = service.getAuthorizationUrl(requestToken);

            resp.sendRedirect(authorizationUrl);
        }
    }

    private class CallbackServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String verifierParam = req.getParameter(VERIFIER_PARAM);
            if (Strings.isNullOrEmpty(verifierParam)) {
                resp.getWriter().println("Failed to login to user account");
                return;
            }

            Verifier verifier = new Verifier(verifierParam);
            Token accessToken = service.getAccessToken(requestToken, verifier);

            printSampleData(resp, accessToken);
        }

        private void printSampleData(HttpServletResponse resp, Token accessToken) throws IOException {
            try {
                List<Note> notes = getSampleData(accessToken);
                for (Note note : notes) {
                    resp.getWriter().println(note);
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().println(e);
            }
        }
    }

    @Override
    public List<Note> getSampleData(Token accessToken) throws Exception {
        List<Note> userNotes = Lists.newArrayList();
        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, accessToken.getToken());
        NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();

        for (Notebook notebook : noteStoreClient.listNotebooks()) {
            NoteFilter filter = new NoteFilter();
            filter.setNotebookGuid(notebook.getGuid());
            filter.setOrder(NoteSortOrder.CREATED.getValue());
            filter.setAscending(true);
            NoteList noteList = noteStoreClient.findNotes(filter, 0, Integer.MAX_VALUE);
            List<Note> notes = noteList.getNotes();

            Path path = Paths.get(String.format("target/%s/%s.txt",APP_NAME, notebook.getGuid()));
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
                Gson gson = new Gson();
                writer.write(gson.toJson(userNotes));
                writer.close();
            }
        }
        
        return userNotes;
    }
}
