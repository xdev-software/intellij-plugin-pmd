/**
 * This packages contain various performance improvements for PMD.
 * <p>
 * Currently:
 * <ul>
 *     <li>
 *         <a href="https://github.com/pmd/pmd/issues/6155">pmd#6155</a>
 *         <ol>
 *             <li>RuleSets: Do not initialize already initialized again</li>
 *             <li>RuleSets: Do not deep copy rules - which causes them to lose their initialization data</li>
 *             <li>SaxonXPathRuleQuery: Reuse cached configuration</li>
 *        </ol>
 *     </li>
 * </ul>
 * </p>
 */
package net.sourceforge.pmd.lang.rule;
