package namwoo2013;

import org.apache.commons.math3.genetics.*;

import java.util.ArrayList;
import java.util.List;

public class RealMutation implements MutationPolicy {

    /**
     * Mutate the given chromosome. Randomly changes one gene.
     * @param original the original chromosome.
     * @return the mutated chromomsome.
     */
    public Chromosome mutate(Chromosome original, double[][] X, double[][] sv, double[] w, double lambda, 
    		double[] mean, double[] std, int[][] INDa, int[][] INDb, int[][] INDc, double obj, double[][] allX) {
        if (!(original instanceof RealChromosome)) {
            throw new IllegalArgumentException("Real mutation works on RealChromosome only.");
        }

        RealChromosome origChrom = (RealChromosome) original;
        List<Double> newRepr = new ArrayList<Double>(origChrom.getRepresentation());

        // randomly select a gene
        int geneIndex = GeneticAlgorithm.getRandomGenerator().nextInt(origChrom.getLength());
        // and change it
        newRepr.set(geneIndex, 
        		Math.min(Math.max(origChrom.getRepresentation().get(geneIndex)+(GeneticAlgorithm.getRandomGenerator().nextDouble()*0.1-0.05),
        				0.0),1.0));        
        Chromosome newChrom = origChrom.newFixedLengthChromosome(newRepr, X, sv, w, lambda, mean, std, INDa, INDb, INDc, obj, allX);
        return newChrom;
    }

}
