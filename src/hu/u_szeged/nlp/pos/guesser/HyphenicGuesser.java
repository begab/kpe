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
 * A HyphenicGuesser osztály kötőjelet tartalmazó tokenek elemzésére szolgál. Feltételezzük, hogy főnevekhez
 * kapcsolt toldalékok kerülnek elemzésre. A kötőjeles szavak két fajtáját kü-lönböztetjük meg. A guesselés első
 * fázisában a kötőjel utáni részt egy toldaláknak (pl.: -nak) tekintve, a MorPhonGuesser segítségével elemezzzük.
 * Például a Bush-hoz szóalak esetén, csak hoz-t használjuk, és illesztjük a talány szótári elem-hez, és kapjuk a
 * Bush@Nc-st elemzést. A második fázisban, a kötőjel utáni részt egy önálló, teljes szóalaknak tekintjük, és
 * önmagá-ban elemezzük. Például Bush-kormányhoz szólak esetében csak a kormányhoz kerül elemzésre, és így kapjuk a
 * Bush-kormány@Nc-sd és Bush-kormány@Nc-sg elemzéseket.
 * 
 * @author zsjanos
 */

public class HyphenicGuesser {

  public static Set<MorAna> guess(String root, String suffix) {

    Set<MorAna> morAnas = null;
    morAnas = new TreeSet<MorAna>();

    // kötőleles suffix (pl.: Bush-hoz)
    morAnas.addAll(MorPhonGuesser.guess(root, suffix));

    // suffix főnév (pl.: Bush-kormánnyal)
    for (String kr : MagyarlancResourceHolder.getRFSA().analyse(suffix)) {
      for (MorAna morAna : MagyarlancResourceHolder.getKRToMSD().getMSD(kr)) {
        // csak fonevi elemzesek
        if (morAna.getMsd().startsWith("N")) {
          morAnas.add(new MorAna(root + "-" + morAna.getLemma(), morAna.getMsd()));
        }
      }
    }
    return morAnas;
  }

  public static void main(String[] args) {
    System.out.println(HyphenicGuesser.guess("Bush", "hoz"));
    System.out.println(HyphenicGuesser.guess("Bush", "kormánynak"));
  }
}
