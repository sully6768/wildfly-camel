/*
* #%L
* Wildfly Camel :: Testsuite
* %%
* Copyright (C) 2013 - 2014 RedHat
* %%
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* #L%
*/
package org.wildfly.camel.test.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;

public class DMRUtils {
    
    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }

        Matcher matcher = Pattern.compile(".*\\((.*?)\\)").matcher(operation);
        if (matcher.matches()) {
            matcher.reset();

            op.get("operation").set(operation.substring(0, operation.indexOf('(')));
            while (matcher.find()) {
                String args = matcher.group(1);
                for (String argSegment : args.split(",")) {
                    String[] argElements = argSegment.split("=");
                    op.get(argElements[0].trim()).set(argElements[1].trim());
                }
            }
        } else {
            op.get("operation").set(operation);
        }
        return op;
    }

    public static ModelNode createCompositeNode(ModelNode[] steps) {
        ModelNode comp = new ModelNode();
        comp.get("operation").set("composite");
        for (ModelNode step : steps) {
            comp.get("steps").add(step);
        }
        return comp;
    }

    public static BatchNodeBuilder batchNode() {
        return new BatchNodeBuilder();
    }

    public static final class BatchNodeBuilder {
        private ModelNode batchNode;

        BatchNodeBuilder() {
            batchNode = new ModelNode();
            batchNode.get("operation").set("composite");
            batchNode.get("address").setEmptyList();
        }

        public BatchNodeBuilder addStep(String address, String operation) {
            batchNode.get("steps").add(createOpNode(address, operation));
            return this;
        }

        public ModelNode build() {
            return batchNode;
        }
    }
}
