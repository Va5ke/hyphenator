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
import model.Token;
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

    private static void fillPhonemeRows(Map<Character, PhoneticTraits> phonemes, List<Map<String, Object>> sonorityData,  List<Map<String, Object>> typeData) {
        for (Map.Entry<Character, PhoneticTraits> entry : phonemes.entrySet()) {
            sonorityData.add(Map.of(
                "symbol", entry.getKey(),
                "sonority", entry.getValue().getSonority()));
            typeData.add(Map.of(
                "symbol", entry.getKey(),
                "type", entry.getValue().getType()));
        }
    }

    private static List<Map<String, Object>> fillSeparatorSonorantsRows() {
        List<Map<String, Object>> data = new ArrayList<>();
        PhonemeType[] types = {PhonemeType.OTHER, PhonemeType.NASAL};
        for (PhonemeType leftType : types) {
            for (PhonemeType rightType : types) {
                data.add(Map.of(
                    "leftType", leftType,
                    "rightType", rightType
                ));
            }
        }
        return data;
    }

    private static List<Map<String, Object>> fillSeparatorPlosiveNasalRows() {
        List<Map<String, Object>> separatorData = new ArrayList<>();
        separatorData.add(Map.of("leftType", PhonemeType.NASAL));
        separatorData.add(Map.of("leftType", PhonemeType.PLOSIVE));
        return separatorData;
    }

    private static String generateRules(ObjectDataCompiler compiler, List<Map<String, Object>> data, String templateName) {
        InputStream phonemeTemplate = App.class.getResourceAsStream("/templates/" + templateName + "Template.drt");
        return compiler.compile(data, phonemeTemplate) + "\n\n";
    }

    private static void writeRules(KieFileSystem kfs, String rulesName) {
        kfs.write("src/main/resources/rules/" + rulesName + "Rules.drl",
          ResourceFactory.newClassPathResource("rules/" + rulesName + "Rules.drl"));
    }

    private static <T extends Token> List<T> extractObjects(KieSession kSession, Class<T> c) {
    return kSession.getObjects().stream()
        .filter(c::isInstance)
        .map(c::cast)
        .sorted(Comparator.comparingInt(Token::getPosition))
        .collect(Collectors.toList());
    }
    
    public static void main(String[] args) throws Exception {

        Map<Character, PhoneticTraits> phonemes = readPhonemes();
        List<Map<String, Object>> sonorityData = new ArrayList<>();
        List<Map<String, Object>> typeData = new ArrayList<>();
        fillPhonemeRows(phonemes, sonorityData, typeData);

        List<Map<String, Object>> separatorSonorantsData = fillSeparatorSonorantsRows();
        List<Map<String, Object>> separatorPlosiveNasalData = fillSeparatorPlosiveNasalRows();

        List<Map<String, Object>> nucleusCandidateData = new ArrayList<>();
        nucleusCandidateData.add(Map.of("symbol", 'л'));
        nucleusCandidateData.add(Map.of("symbol", 'н'));
        nucleusCandidateData.add(Map.of("symbol", 'р'));

        ObjectDataCompiler compiler = new ObjectDataCompiler();
        String combinedRules = "";
        combinedRules += generateRules(compiler, sonorityData, "sonority");
        combinedRules += generateRules(compiler, typeData, "type");
        combinedRules += generateRules(compiler, separatorSonorantsData, "separatorSonorants");
        combinedRules += generateRules(compiler, separatorPlosiveNasalData, "separatorPlosiveNasal");
        combinedRules += generateRules(compiler, nucleusCandidateData, "nucleusCandidate");
        
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write("src/main/resources/rules/generatedRules.drl", combinedRules);
        
        writeRules(kfs, "nucleus");
        writeRules(kfs, "separator");
        writeRules(kfs, "hyphenation");

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException(kb.getResults().toString());
        }
        
        KieContainer kContainer = ks.newKieContainer(kb.getKieModule().getReleaseId());
        KieSession kSession = kContainer.newKieSession();

        String word = "седамнаестогодишњакиња";
        for (int i=0; i<word.length(); i++) {
            Letter l = new Letter(i, word.charAt(i));
            kSession.insert(l);
        }

        kSession.fireAllRules();

        List<Letter> processedLetters = extractObjects(kSession, Letter.class);
        List<Separator> separators = extractObjects(kSession, Separator.class);

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
