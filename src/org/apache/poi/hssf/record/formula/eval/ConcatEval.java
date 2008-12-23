/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hssf.record.formula.eval;

import org.apache.poi.hssf.record.formula.ConcatPtg;
import org.apache.poi.hssf.record.formula.Ptg;

/**
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt;
 *  
 */
public final class ConcatEval implements OperationEval {

    private ConcatPtg delegate;

    public ConcatEval(Ptg ptg) {
        this.delegate = (ConcatPtg) ptg;
    }

    public Eval evaluate(Eval[] args, int srcRow, short srcCol) {
    	if(args.length != 2) {
    		return ErrorEval.VALUE_INVALID;
    	}
        StringBuffer sb = new StringBuffer();
        try {
			for (int i = 0; i < 2; i++) { 
			    
			    ValueEval ve = OperandResolver.getSingleValue(args[i], srcRow, srcCol);
			    if (ve instanceof StringValueEval) {
			        StringValueEval sve = (StringValueEval) ve;
			        sb.append(sve.getStringValue());
			    } else if (ve == BlankEval.INSTANCE) {
			        // do nothing
			    } else { // must be an error eval
			        throw new RuntimeException("Unexpected value type (" 
			        		+ ve.getClass().getName() + ")");
			    }
			}
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
        
        return new StringEval(sb.toString());
    }

    public int getNumberOfOperands() {
        return delegate.getNumberOfOperands();
    }

    public int getType() {
        return delegate.getType();
    }
}
