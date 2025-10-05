package com.ftn.sbnz.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import com.ftn.sbnz.model.events.SpaceEvent;
import com.ftn.sbnz.model.events.SymbolEvent;
import com.ftn.sbnz.model.models.PhonemeType;
import com.ftn.sbnz.model.models.PhoneticTraits;
import com.ftn.sbnz.model.models.Text;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;


@Service
public class TextService {

    private int MAX_ROW_LENGTH = 9;
    private String PHONEMES_PATH = "csv/phonemes.csv";

    private List<Character> alphabet = List.of(
        'а', 'б', 'в', 'г', 'д', 'ђ', 'е', 'ж', 'з', 'и', 'ј', 'к', 'л',
        'љ', 'м', 'н', 'њ', 'о', 'п', 'р', 'с', 'т', 'ћ', 'у', 'ф', 'х',
        'ц', 'ч', 'џ', 'ш'
    );

    private KieSession kSession;
    private Text text;

    private Map<Character, PhoneticTraits> readPhonemes() throws IOException {
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

    private Map<Character, PhoneticTraits> parseCsv(BufferedReader br) throws IOException {
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

    private void fillPhonemeRows(Map<Character, PhoneticTraits> phonemes, List<Map<String, Object>> sonorityData,  List<Map<String, Object>> typeData) {
        for (Map.Entry<Character, PhoneticTraits> entry : phonemes.entrySet()) {
            sonorityData.add(Map.of(
                "symbol", entry.getKey(),
                "sonority", entry.getValue().getSonority()));
            typeData.add(Map.of(
                "symbol", entry.getKey(),
                "type", entry.getValue().getType()));
        }
    }

    private List<Map<String, Object>> fillSeparatorSonorantsRows() {
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

    private List<Map<String, Object>> fillSeparatorPlosiveNasalRows() {
        List<Map<String, Object>> separatorData = new ArrayList<>();
        separatorData.add(Map.of("leftType", PhonemeType.NASAL));
        separatorData.add(Map.of("leftType", PhonemeType.PLOSIVE));
        return separatorData;
    }

    private String generateRules(ObjectDataCompiler compiler, List<Map<String, Object>> data, String templateName) {
        InputStream phonemeTemplate = App.class.getResourceAsStream("/templates/" + templateName + "Template.drt");
        return compiler.compile(data, phonemeTemplate) + "\n\n";
    }

    private void writeRules(KieFileSystem kfs, String rulesName) {
        kfs.write("src/main/resources/rules/" + rulesName + "Rules.drl",
          ResourceFactory.newClassPathResource("rules/" + rulesName + "Rules.drl"));
    }

    @PostConstruct
    public void init() throws Exception {
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

        int windowSize = MAX_ROW_LENGTH + 1;
        List<Map<String, Object>> cepData = List.of(Map.of("windowSize", windowSize));

        ObjectDataCompiler compiler = new ObjectDataCompiler();
        String combinedRules =
            "declare window TextEvents\n" +
            "    com.ftn.sbnz.model.events.TextEvent() over window:length(" + windowSize + ")\n" +
            "end\n\n" +
            "declare window Recent\n" +
            "    com.ftn.sbnz.model.events.TextEvent() over window:length(1)\n" +
            "end\n\n";
        combinedRules += generateRules(compiler, sonorityData, "sonority");
        combinedRules += generateRules(compiler, typeData, "type");
        combinedRules += generateRules(compiler, separatorSonorantsData, "separatorSonorants");
        combinedRules += generateRules(compiler, separatorPlosiveNasalData, "separatorPlosiveNasal");
        combinedRules += generateRules(compiler, nucleusCandidateData, "nucleusCandidate");
        combinedRules += generateRules(compiler, cepData, "hyphenation");

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

        KieBaseConfiguration kbConf = ks.newKieBaseConfiguration();
        kbConf.setOption(EventProcessingOption.STREAM);
        KieContainer kContainer = ks.newKieContainer(kb.getKieModule().getReleaseId());
        KieBase kBase = kContainer.newKieBase(kbConf);

        kSession = kBase.newKieSession();
        text = new Text();
        kSession.insert(text);
    }

    public String processEvent(int num) {
        if (num == 0) {
            kSession.insert(new SpaceEvent());
        } else if (num >= 1 && num <= alphabet.size()) {
            char c = alphabet.get(num - 1);
            kSession.insert(new SymbolEvent(c));
        }
        kSession.fireAllRules();
        return text.toString();
    }

    @PreDestroy
    public void destroy() {
        if (kSession != null) kSession.dispose();
    }
}