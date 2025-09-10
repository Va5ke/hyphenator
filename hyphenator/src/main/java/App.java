import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import model.Letter;
import model.PhoneticTraits;
import model.Separator;
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
        List<Map<String, Object>> nucleusCandidateData = new ArrayList<>();
        nucleusCandidateData.add(Map.of("symbol", 'л'));
        nucleusCandidateData.add(Map.of("symbol", 'н'));
        nucleusCandidateData.add(Map.of("symbol", 'р'));

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

        InputStream nucleusCandidateTemplate = App.class.getResourceAsStream("/templates/nucleusCandidateTemplate.drt");
        String nucleusCandidateRules = compiler.compile(nucleusCandidateData, nucleusCandidateTemplate);

        String combinedRules = sonorityRules + "\n\n" + typeRules + "\n\n" + nucleusCandidateRules;
        
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write("src/main/resources/rules/generatedRules.drl", combinedRules);
        kfs.write("src/main/resources/rules/nucleusRules.drl",
          ResourceFactory.newClassPathResource("rules/nucleusRules.drl"));
        kfs.write("src/main/resources/rules/separatorRules.drl",
          ResourceFactory.newClassPathResource("rules/separatorRules.drl"));
        kfs.write("src/main/resources/rules/hyphenationRules.drl",
          ResourceFactory.newClassPathResource("rules/hyphenationRules.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException(kb.getResults().toString());
        }
        
        KieContainer kContainer = ks.newKieContainer(kb.getKieModule().getReleaseId());
        KieSession kSession = kContainer.newKieSession();

        String word = "деветнаестогодишњакиња";
        // String word = "шаптати";
        // String word = "стално";
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

        List<Separator> separators = kSession.getObjects().stream()
            .filter(o -> o instanceof Separator)
            .map(o -> (Separator) o)
            .sorted(Comparator.comparingInt(Separator::getPosition))
            .collect(Collectors.toList());

        int separatorCounter = 0;
        for (Letter l : processedLetters) {
            System.out.print(l.getSymbol());
            if (separatorCounter == separators.size() ||
                separators.get(separatorCounter).getPosition() != l.getPosition()) continue;
            System.out.print(separators.get(separatorCounter).isValid() ? "|" : ":");
            separatorCounter++;
        }

        kSession.dispose();
    }
}
