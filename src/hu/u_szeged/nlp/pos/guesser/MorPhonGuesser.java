package hu.u_szeged.nlp.pos.guesser;
/**
 * Developed by:
 *   Research Group on Artificial Intelligence of the Hungarian Academy of Sciences
 *   http://www.inf.u-szeged.hu/rgai/
 *
 * Contact:
 *  János Zsibrita
 *  zsibrita@inf.u-szeged.hu
 *  
 * Licensed by Creative Commons Attribution Share Alike
 *  
 * http://creativecommons.org/licenses/by-sa/3.0/legalcode
 */

import hu.u_szeged.nlp.pos.MagyarlancResourceHolder;
import hu.u_szeged.nlp.pos.MorAna;

import java.util.Set;
import java.util.TreeSet;

/**
 * A MorPhonGuesser osztály egy ismeretlen (nem elemezhető) főnévi szótő és
 * tetszőleges suffix guesselésére szolgál. A guesselés során az adott suffixet
 * a rendszer morPhonDir szótá-rának elemeire illesztve próbájuk elemezni. A
 * szótár reprezentálja a magyar nyelv minden (nem hasonuló) illeszkedési
 * szabályát, így biztosak lehetünk benne, hogy egy valós toldalék mindenképp
 * illeszkedni fog legalább egy szótárelemre. Például egy „hoz” rag esetén,
 * először a köd elemre próbálunk illeszteni, majd elemezni. A kapott szóalak
 * így a ködhez lesz, melyre a KR elemzőnk nem ad elemzést. A következő
 * szó-tárelem a talány, a szóalak a talányhoz lesz, melyre megkapjuk az Nc-st
 * (külső közelí-tő/allative) főnévi elemzést.
 */
public class MorPhonGuesser {
  
  public static Set<MorAna> guess(String root, String suffix) {
    
    Set<MorAna> stems = null;
    stems = new TreeSet<MorAna>();
    
    for (String guess : MagyarlancResourceHolder.getMorPhonDir()) {
      if (MagyarlancResourceHolder.getRFSA().analyse(guess + suffix).size() > 0) {
        for (String kr : MagyarlancResourceHolder.getRFSA().analyse(
            guess + suffix)) {
          for (MorAna stem : MagyarlancResourceHolder.getKRToMSD().getMSD(kr)) {
            if (stem.getMsd().startsWith("N")) {
              stems.add(new MorAna(root, stem.getMsd()));
            }
          }
        }
      }
    }
    
    return stems;
  }
  
  public static void main(String[] args) {
    System.out.println(guess("London", "ban"));
  }
}
