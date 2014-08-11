package hu.u_szeged.nlp.pos;

public class MorAna implements Comparable<MorAna> {

  private String lemma = null;
  private String msd = null;

  public MorAna() {
    this.lemma = null;
    this.msd = null;
  }

  public MorAna(String lemma, String msd) {
    this.setLemma(lemma);
    this.setMsd(msd);
  }

  public String toString() {
    return this.getLemma() + "@" + this.getMsd();
  }

  public String getLemma() {
    return this.lemma;
  }

  public String getMsd() {
    return this.msd;
  }

  public void setLemma(String lemma) {
    this.lemma = lemma;
  }

  public void setMsd(String msd) {
    this.msd = msd;
  }

  @Override
  public int compareTo(MorAna morAna) {
    // megegyezik az lemma és az MSD is
    if (this.getLemma().equals(morAna.getLemma()) && this.getMsd().equals(morAna.getMsd()))
      return 0;

    // megegyezik az lemma
    if (this.getLemma().equals(((MorAna) morAna).getLemma()))
      return this.getMsd().compareTo(((MorAna) morAna).getMsd());

    else
      return this.getLemma().compareTo(((MorAna) morAna).getLemma());
  }

  public boolean equals(MorAna morAna) {
    if (this.toString().equals(morAna.toString())) {
      return true;
    }
    return false;
  }

  public static void main(String[] args) {
    MorAna s1 = new MorAna("1250-es", "Nn-sn");
    MorAna s2 = new MorAna("1250-es", "Nn-sn");

    System.out.println(s1.compareTo(s2));
    System.out.println(s1.equals(s2));
  }

}
