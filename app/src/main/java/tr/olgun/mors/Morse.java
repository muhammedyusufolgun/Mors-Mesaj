package tr.olgun.mors;

import java.util.*;
import java.text.Normalizer;

final class Morse {
    static final int UNIT = 240;
    static final Map<Character,String> codes = new HashMap<>();
    static final Map<String,Character> letters = new HashMap<>();
    static {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ÇĞİÖŞÜ.,?!-/'():@=";
        String[] values = (".- -... -.-. -.. . ..-. --. .... .. .--- -.- .-.. -- -. --- .--. --.- .-. ... - ..- ...- .-- -..- -.-- --.. " +
            "----- .---- ..--- ...-- ....- ..... -.... --... ---.. ----. " +
            "-.-.. --.-. ..-.. ---. ---- ..-- .-.-.- --..-- ..--.. -.-.-- -....- -..-. .----. -.--. -.--.- ---... .--.-. -...-").split(" ");
        for (int i=0; i<alphabet.length(); i++) {
            codes.put(alphabet.charAt(i), values[i]);
            letters.put(values[i], alphabet.charAt(i));
        }
    }
    static String encode(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i=0; i<normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) { space = out.length()>0; continue; }
            c = c=='i' ? 'İ' : Character.toUpperCase(c);
            String code = codes.get(c);
            if (code==null) throw new IllegalArgumentException("Desteklenmeyen karakter: " + new String(Character.toChars(normalized.codePointAt(i))));
            if (out.length()>0) out.append(space ? " / " : " ");
            out.append(code);
            space=false;
        }
        return out.toString();
    }
    // Signed durations in dot units. Negative values turn the torch off.
    static List<Integer> pulses(String code) {
        List<Integer> parts=new ArrayList<>();
        for (char c:code.toCharArray()) {
            if (c=='.' || c=='-') {
                parts.add(c=='.' ? 1 : 3);
                parts.add(-1);
            } else if (c=='/') parts.set(parts.size()-1,-7);
            else if (parts.get(parts.size()-1)!=-7) parts.set(parts.size()-1,-3);
        }
        if (!parts.isEmpty()) parts.remove(parts.size()-1);
        return parts;
    }
    static final class Decoder {
        boolean lit, pendingSpace;
        long edge;
        StringBuilder symbols=new StringBuilder(), message=new StringBuilder();
        void sample(boolean light,long now) {
            long dt=now-edge;
            if (!lit) {
                if (dt>=UNIT*2 && symbols.length()>0) {
                    Character letter=letters.get(symbols.toString());
                    message.append(letter==null ? '�' : letter.charValue());
                    symbols.setLength(0);
                }
                if (dt>=UNIT*5 && message.length()>0) pendingSpace=true;
            }
            if (light==lit) return;
            if (lit && dt>=UNIT/3) {
                if (pendingSpace) { message.append(' '); pendingSpace=false; }
                symbols.append(dt>=UNIT*2 ? '-' : '.');
            }
            edge=now;
            lit=light;
        }
    }
}
