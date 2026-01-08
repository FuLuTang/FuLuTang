package Algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Modele.Etudiant;
import Modele.Groupe;

public class AlgoHaochen {
    public ArrayList<Groupe> desGroupes;

    public ArrayList<Groupe> routeur(List<Etudiant> listEtudiants, int mode) {
        switch (mode) {
            case 1:
                return this.ConstitutionGlouton1(listEtudiants);
            case 2:
                return this.ConstitutionGlouton2(listEtudiants);
            case 3:
                return this.ConstitutionForceBrute(listEtudiants);
        }
        return null;
    }

    public ArrayList<Groupe> ConstitutionGlouton1(List<Etudiant> listEtudiants) {
        this.desGroupes = new ArrayList<Groupe>();
        ArrayList<Etudiant> listEtudiantsAnglais = new ArrayList<Etudiant>();
        ArrayList<Etudiant> listEtudiantsFrancais = new ArrayList<Etudiant>();

        // Séparation des étudiants en fonction de leur cours d'anglais
        for (Etudiant etudiant : listEtudiants) {
            if (etudiant.estCoursAnglais()) {
                listEtudiantsAnglais.add(etudiant);
            } else {
                listEtudiantsFrancais.add(etudiant);
            }
        }

        // Calcul du nombre de groupes (cible ~24 étudiants)
        int nbGroupeAnglais = (int) Math.ceil(listEtudiantsAnglais.size() / 24.0);
        int nbGroupeFrancais = (int) Math.ceil(listEtudiantsFrancais.size() / 24.0);

        ArrayList<Groupe> listGroupesAnglais = new ArrayList<Groupe>();
        ArrayList<Groupe> listGroupesFrancais = new ArrayList<Groupe>();

        char code = 'A';
        for (int i = 0; i < nbGroupeAnglais; i++)
            listGroupesAnglais.add(new Groupe(code++));
        for (int i = 0; i < nbGroupeFrancais; i++)
            listGroupesFrancais.add(new Groupe(code++));

        // Traiter les deux langues séparément
        distribuerEnSerpentin(listEtudiantsAnglais, listGroupesAnglais);
        distribuerEnSerpentin(listEtudiantsFrancais, listGroupesFrancais);

        this.desGroupes.addAll(listGroupesAnglais);
        this.desGroupes.addAll(listGroupesFrancais);
        return this.desGroupes;
    }

    /**
     * Méthode helper pour distribuer les étudiants en "S" (Snake)
     * Gère la séparation Redoublants / Ordinaires
     */
    private void distribuerEnSerpentin(List<Etudiant> etudiants, List<Groupe> groupes) {
        if (groupes.isEmpty())
            return;

        ArrayList<Etudiant> redoublants = new ArrayList<>();
        ArrayList<Etudiant> ordinaires = new ArrayList<>();

        for (Etudiant e : etudiants) {
            if (estRedoublant(e))
                redoublants.add(e);
            else
                ordinaires.add(e);
        }

        // Tri par note décroissante (Meilleurs d'abord, pour le S)
        redoublants.sort((e1, e2) -> Float.compare(e2.getNote(), e1.getNote()));
        ordinaires.sort((e1, e2) -> Float.compare(e2.getNote(), e1.getNote()));

        // On distribue d'abord les redoublants, puis les ordinaires
        int ordre = 0;
        int direction = 1;

        ArrayList<Etudiant> fileDAttente = new ArrayList<>();
        fileDAttente.addAll(redoublants);
        fileDAttente.addAll(ordinaires);

        for (Etudiant etudiant : fileDAttente) {
            groupes.get(ordre).ajouteEtudiant(etudiant);

            if (groupes.size() > 1) {
                if (ordre == 0 && direction == -1) {
                    direction = 1;
                } else if (ordre == groupes.size() - 1 && direction == 1) {
                    direction = -1;
                } else {
                    ordre += direction;
                }
            }
        }
    }

    public ArrayList<Groupe> ConstitutionGlouton2(List<Etudiant> listEtudiants) {
        this.desGroupes = new ArrayList<Groupe>();
        char code = 'A';
        ArrayList<Etudiant> anglais = new ArrayList<>();
        ArrayList<Etudiant> autres = new ArrayList<>();

        for (Etudiant e : listEtudiants) {
            if (e.estCoursAnglais())
                anglais.add(e);
            else
                autres.add(e);
        }

        // 1. Gérer l'anglais (simplement via Glouton 1 ou direct)
        int nbAnglais = (int) Math.ceil(anglais.size() / 24.0);
        ArrayList<Groupe> grpAnglais = new ArrayList<>();
        for (int i = 0; i < nbAnglais; i++)
            grpAnglais.add(new Groupe(code++));

        // Pour l'anglais, on garde le Snake qui est robuste
        distribuerEnSerpentin(anglais, grpAnglais);

        // 2. Gérer les autres avec l'algo dynamique (moyenne la plus faible)
        int nbFrancais = (int) Math.ceil(autres.size() / 24.0);
        ArrayList<Groupe> grpFrancais = new ArrayList<>();
        for (int i = 0; i < nbFrancais; i++)
            grpFrancais.add(new Groupe(code++));

        // TRIER DU PLUS BAS AU PLUS HAUT (Ascenseur / Water Filling)
        // L'idée est de remplir les "creux" (groupes faibles) avec les meilleurs
        autres.sort((e1, e2) -> Float.compare(e1.getNote(), e2.getNote()));

        for (Etudiant e : autres) {
            // Trouver le groupe avec la moyenne la plus basse
            // Trouver le groupe avec la moyenne la plus basse
            Groupe cible = null;
            float minMoyenne = Float.MAX_VALUE;

            for (Groupe g : grpFrancais) {
                if (!g.peutAjouter())
                    continue; // Skip full groups

                float moy = noteMoyenne(g.getListEtudiants());
                if (moy < minMoyenne) {
                    minMoyenne = moy;
                    cible = g;
                }
            }

            if (cible != null) {
                cible.ajouteEtudiant(e);
            } else {
                // Fallback: This should ideally not happen if max capacity * nbGroups >=
                // nbStudents
                // But if it does, force add to first non-full or throw error
                System.err.println("Warning: Could not find non-full group for Student " + e.getIdEtudiant());
            }
        }

        this.desGroupes.addAll(grpAnglais);
        this.desGroupes.addAll(grpFrancais);
        return this.desGroupes;
    }

    public ArrayList<Groupe> ConstitutionForceBrute(List<Etudiant> listEtudiants) {
        ArrayList<Groupe> bestGroupes = null;
        double minScore = Double.MAX_VALUE;

        // Separate English / non-English
        ArrayList<Etudiant> listEtudiantsAnglais = new ArrayList<>();
        ArrayList<Etudiant> listEtudiantsFrancais = new ArrayList<>();
        for (Etudiant e : listEtudiants) {
            if (e.estCoursAnglais())
                listEtudiantsAnglais.add(e);
            else
                listEtudiantsFrancais.add(e);
        }

        // Determine group counts (fixed logic as per Glouton 1)
        int nbGroupeAnglais = (int) Math.ceil(listEtudiantsAnglais.size() / 24.0);
        int nbGroupeFrancais = (int) Math.ceil(listEtudiantsFrancais.size() / 24.0);

        // Run Monte Carlo 1000 times
        int iterations = 1000;
        for (int k = 0; k < iterations; k++) {
            ArrayList<Groupe> currentGroupes = new ArrayList<>();
            ArrayList<Groupe> grpAnglais = new ArrayList<>();
            ArrayList<Groupe> grpFrancais = new ArrayList<>();

            // Build fresh groups
            char code = 'A';
            for (int i = 0; i < nbGroupeAnglais; i++)
                grpAnglais.add(new Groupe(code++));
            for (int i = 0; i < nbGroupeFrancais; i++)
                grpFrancais.add(new Groupe(code++));

            // Shuffle students randomly
            ArrayList<Etudiant> shuffledAnglais = new ArrayList<>(listEtudiantsAnglais);
            ArrayList<Etudiant> shuffledFrancais = new ArrayList<>(listEtudiantsFrancais);
            Collections.shuffle(shuffledAnglais);
            Collections.shuffle(shuffledFrancais);

            // Simple cyclic distribution to fill groups randomly
            distribuerCyclique(shuffledAnglais, grpAnglais);
            distribuerCyclique(shuffledFrancais, grpFrancais);

            currentGroupes.addAll(grpAnglais);
            currentGroupes.addAll(grpFrancais);

            // Evaluate Score (lower is better)
            double score = evaluerDistribution(currentGroupes);
            if (score < minScore) {
                minScore = score;
                bestGroupes = currentGroupes;
            }
        }

        this.desGroupes = bestGroupes;
        return this.desGroupes;
    }

    private void distribuerCyclique(ArrayList<Etudiant> etudiants, ArrayList<Groupe> groupes) {
        if (groupes.isEmpty())
            return;
        int index = 0;
        for (Etudiant e : etudiants) {
            // Find a group that isn't full starting from index
            int steps = 0;
            while (steps < groupes.size()) {
                Groupe g = groupes.get(index);
                if (g.peutAjouter()) {
                    g.ajouteEtudiant(e);
                    index = (index + 1) % groupes.size();
                    break;
                }
                index = (index + 1) % groupes.size();
                steps++;
            }
        }
    }

    private double evaluerDistribution(ArrayList<Groupe> groupes) {
        if (groupes.isEmpty())
            return Double.MAX_VALUE;

        // Metric 1: Variance of Average Notes
        double sumAvg = 0;
        double sumSqAvg = 0;
        int count = 0;

        // Metric 2: Max difference in Redoublants
        int maxRed = Integer.MIN_VALUE;
        int minRed = Integer.MAX_VALUE;

        for (Groupe g : groupes) {
            float avg = noteMoyenne(g.getListEtudiants());
            sumAvg += avg;
            sumSqAvg += avg * avg;
            count++;

            int nbRed = 0;
            for (Etudiant e : g.getListEtudiants()) {
                if (estRedoublant(e))
                    nbRed++;
            }
            if (nbRed > maxRed)
                maxRed = nbRed;
            if (nbRed < minRed)
                minRed = nbRed;
        }

        double meanOfAvgs = sumAvg / count;
        double variance = (sumSqAvg / count) - (meanOfAvgs * meanOfAvgs);

        // Score = Variance + Penalty * RedoublantDiff
        // We want to minimize both. Variance is usually small (e.g., 0.5 - 2.0).
        // RedoublantDiff is integer (0, 1, 2...).
        // Give some weight to redoublant balance.
        double redoublantDiff = (maxRed - minRed);

        return variance + (redoublantDiff * 0.5);
    }

    public boolean estRedoublant(Etudiant e) {
        // Basé sur statutAcademique ou etatAcademique (à vérifier selon la DB)
        return e.getEtatAcademique() != null && e.getEtatAcademique().toLowerCase().contains("redoublant");
    }

    public float noteMoyenne(List<Etudiant> listEtudiants) {
        if (listEtudiants == null || listEtudiants.isEmpty())
            return 0;
        float noteMoyenne = 0;
        for (Etudiant etudiant : listEtudiants) {
            noteMoyenne += etudiant.getNote();
        }
        return noteMoyenne / listEtudiants.size();
    }
}
