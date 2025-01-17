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

/**
 * Algorithm used to mutate a chrommosome.
 *
 * @since 2.0
 * @version $Revision: 811685 $ $Date: 2009-09-05 19:36:48 +0200 (sam. 05 sept. 2009) $
 */
public interface MutationPolicy {

    /**
     * Mutate the given chromosome.
     * @param original the original chromosome.
     * @param R 
     * @param allX 
     * @param param 
     * @param model 
     * @return the mutated chromomsome.
     */
    Chromosome mutate(Chromosome original, double[][] X, double[][] SV, double[] w, 
    		double lambda, double[] mean, double[] std, int[][] INDa, int[][] INDb, int[][] INDc, double obj, double[][] allX);
}
