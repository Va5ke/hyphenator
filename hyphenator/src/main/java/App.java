import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.bm.Rule.Phoneme;
import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import model.Letter;
import model.PhoneticTraits;
import model.PhonemeType;

public class App {

    private static final String PHONEMES_PATH = "csv/phonemes.csv";

    private static Map<Character, PhoneticTraits> readPhonemes() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(PHONEMES_PATH)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found on classpath: csv/phonemes.csv");
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return parseCsv(br);
            }
        }
    }

    private static Map<Character, PhoneticTraits> parseCsv(BufferedReader br) throws IOException {
        Map<Character, PhoneticTraits> phonemes = new HashMap<Character, PhoneticTraits>();
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            if (line.isBlank()) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            char letter = parts[0].trim().charAt(0);
            PhonemeType type = PhonemeType.valueOf(parts[1].trim());
            int sonority = Integer.parseInt(parts[2].trim());
            phonemes.put(letter, new PhoneticTraits(type, sonority));
        }
        return phonemes;
    }
    
    public static void main(String[] args) throws Exception {

        Map<Character, PhoneticTraits> phonemes = readPhonemes();

        List<Map<String, Object>> sonorityData = new ArrayList<>();
        List<Map<String, Object>> typeData = new ArrayList<>();

        for (Map.Entry<Character, PhoneticTraits> entry : phonemes.entrySet()) {
            char symbol = entry.getKey();
            PhoneticTraits traits = entry.getValue();

            Map<String, Object> sRow = new HashMap<>();
            sRow.put("symbol", symbol);
            sRow.put("sonority", traits.getSonority());
            sonorityData.add(sRow);

            Map<String, Object> tRow = new HashMap<>();
            tRow.put("symbol", symbol);
            tRow.put("type", traits.getType());
            typeData.add(tRow);
        }

        ObjectDataCompiler compiler = new ObjectDataCompiler();

        InputStream sonorityTemplate = App.class.getResourceAsStream("/templates/sonorityTemplate.drt");
        String sonorityRules = compiler.compile(sonorityData, sonorityTemplate);

        InputStream typeTemplate = App.class.getResourceAsStream("/templates/typeTemplate.drt");
        String typeRules = compiler.compile(typeData, typeTemplate);

        String combinedRules = sonorityRules + "\n\n" + typeRules;
        
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write("src/main/resources/rules/generatedRules.drl", combinedRules);
        kfs.write("src/main/resources/rules/vowelRule.drl",
          ResourceFactory.newClassPathResource("rules/vowelRule.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException(kb.getResults().toString());
        }
        
        KieContainer kContainer = ks.newKieContainer(kb.getKieModule().getReleaseId());
        KieSession kSession = kContainer.newKieSession();

        String word = "обезвредити".toLowerCase();
        for (int i=0; i<word.length(); i++) {
            Letter l = new Letter(i, word.charAt(i));
            kSession.insert(l);
        }

        kSession.fireAllRules();

        List<Letter> processedLetters = kSession.getObjects().stream()
        .filter(o -> o instanceof Letter)
        .map(o -> (Letter) o)
        .sorted(Comparator.comparingInt(Letter::getPosition))
        .collect(Collectors.toList());

        for (Letter l: processedLetters) {
            System.out.println(l);
        }

        kSession.dispose();
    }
}
