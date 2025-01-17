/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package namwoo2013;

import org.apache.commons.math3.genetics.*;

import java.util.ArrayList;
import java.util.List;


/**
 * One point crossover policy. A random crossover point is selected and the
 * first part from each parent is copied to the corresponding child, and the
 * second parts are copied crosswise.
 *
 * Example:
 * <pre>
 * -C- denotes a crossover point
 *                   -C-                                -C-
 * p1 = (1 0 1 0 0 1  | 0 1 1)    X    p2 = (0 1 1 0 1 0  | 1 1 1)
 *         \------------/ \-----/              \------------/ \-----/
 *            ||         (*)                       ||        (**)
 *            VV         (**)                      VV        (*)
 *      /------------\ /-----\              /------------\ /-----\
 * c1 = (1 0 1 0 0 1  | 1 1 1)    X    p2 = (0 1 1 0 1 0  | 0 1 1)
 * </pre>
 *
 * This policy works only on {@link AbstractListChromosome}, and therefore it
 * is parametrized by T. Moreover, the chromosomes must have same lengths.
 *
 * @param <T> generic type of the {@link AbstractListChromosome}s for crossover
 * @since 2.0
 * @version $Revision: 903046 $ $Date: 2010-01-26 03:07:26 +0100 (mar. 26 janv. 2010) $
 *
 */
public class OnePointCrossover<T> implements CrossoverPolicy {

    /**
     * Performs one point crossover. A random crossover point is selected and the
     * first part from each parent is copied to the corresponding child, and the
     * second parts are copied crosswise.
     *
     * Example:
     * -C- denotes a crossover point
     *                   -C-                                -C-
     * p1 = (1 0 1 0 0 1  | 0 1 1)    X    p2 = (0 1 1 0 1 0  | 1 1 1)
     *         \------------/ \-----/              \------------/ \-----/
     *            ||         (*)                       ||        (**)
     *            VV         (**)                      VV        (*)
     *      /------------\ /-----\              /------------\ /-----\
     * c1 = (1 0 1 0 0 1  | 1 1 1)    X    p2 = (0 1 1 0 1 0  | 0 1 1)
     *
     * @param first first parent (p1)
     * @param second second parent (p2)
     * @return pair of two children (c1,c2)
     */
    @SuppressWarnings("unchecked") // OK because of instanceof checks
    public ChromosomePair crossover(Chromosome first, Chromosome second, 
    		double[][] X, double[][] sv, double[] w, double lambda, double[] mean, double[] std, 
    		int[][] INDa, int[][] INDb, int[][] INDc, 
    		double obj, double[][] allX) {
        if (! (first instanceof AbstractListChromosome<?> && second instanceof AbstractListChromosome<?>)) {
            throw new IllegalArgumentException("One point crossover works on FixedLengthChromosomes only.");
        }
        return crossover((AbstractListChromosome<T>) first, (AbstractListChromosome<T>) second, 
        		X, sv, w, lambda, mean, std, INDa, INDb, INDc, obj, allX);
    }


    /**
     * Helper for {@link #crossover(Chromosome, Chromosome)}. Performs the actual crossover.
     *
     * @param first the first chromosome.
     * @param second the second chromosome.
     * @return the pair of new chromosomes that resulted from the crossover.
     */
    private ChromosomePair crossover(AbstractListChromosome<T> first, AbstractListChromosome<T> second, 
    		double[][] X, double[][] sv, double[] w, double lambda, double[] mean, double[] std, 
    		int[][] INDa, int[][] INDb, int[][] INDc,
    		double obj, double[][] allX) {
        int length = first.getLength();
        if (length != second.getLength())
            throw new IllegalArgumentException("Both chromosomes must have same lengths.");

        // array representations of the parents
        List<T> parent1Rep = first.getRepresentation();
        List<T> parent2Rep = second.getRepresentation();

        // and of the children
        ArrayList<T> child1Rep = new ArrayList<T> (first.getLength());
        ArrayList<T> child2Rep = new ArrayList<T> (second.getLength());

        // select a crossover point at random (0 and length makes no sense)
        int crossoverIndex = 1 + (GeneticAlgorithm.getRandomGenerator().nextInt(length-2));

        // copy the first part
        for (int i = 0; i < crossoverIndex; i++) {
            child1Rep.add(parent1Rep.get(i));
            child2Rep.add(parent2Rep.get(i));
        }
        // and switch the second part
        for (int i = crossoverIndex; i < length; i++) {
            child1Rep.add(parent2Rep.get(i));
            child2Rep.add(parent1Rep.get(i));
        }
        
        return new ChromosomePair(
                first.newFixedLengthChromosome(child1Rep, X, sv, w, lambda, mean, std, INDa, INDb, INDc, obj, allX),
                second.newFixedLengthChromosome(child2Rep, X, sv, w, lambda, mean, std, INDa, INDb, INDc, obj, allX)
                );
    }

}
