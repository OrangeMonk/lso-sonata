/*Copyright 2017 Cataworx, Inc

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/


package com.cataworx.mef.lso.sonata;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonObject;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;



@RestController
public class OrderController {

    // Return messages used to test the flow.
    private static final String SUCCESS = "Your Order Has Been Processed.";
    private static final String FAILURE = "Your Order Could Not Be Processed.";

    //URL that points Opendaylight instance running UNI Manager
    //TODO: Extract the URL and access information into external configuration parameters
    private static final String urlString = "http://96.86.165.72:8181/restconf/operations/tapi-connectivity:create-connectivity-service";

    @RequestMapping("/order/eaccess")
    public HttpEntity<OrderResponse> processOrder(
            @RequestParam(value = "orderID", required = false, defaultValue = "1") String orderID) {

        // Construct JSON payload based on UNI Manager's Data Model
        // Used indentation to highlight object structure
        //TODO: Modify this request to work at the LEGATO interface level
        //TODO: Increase the level of abastraction of the request. Some of these parameters belong in PRESTO
        JsonObject payload = Json.createObjectBuilder()
                .add("input", Json.createObjectBuilder()
                        .add("end-point", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("service-interface-point", "sip:ovs-node:s5:s5-eth1")
                                        .add("direction", "bidirectional")
                                        .add("layer-protocol-name", "eth")
                                        .add("nrp-cg-eth-frame-flow-cpa-aspec", Json.createObjectBuilder()
                                                .add("ce-vlan-id-list",Json.createObjectBuilder()
                                                        .add("vlan-id-list", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("vlan-id", 202))))))

                                .add(Json.createObjectBuilder()
                                        .add("service-interface-point", "sip:ovs-node:s1:ens6")
                                        .add("direction", "bidirectional")
                                        .add("layer-protocol-name", "eth")
                                        .add("nrp-cg-eth-frame-flow-cpa-aspec", Json.createObjectBuilder()
                                                .add("ce-vlan-id-list", Json.createObjectBuilder()
                                                        .add("vlan-id-list", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("vlan-id", 202)))))))

        .add("conn-constraint",Json.createObjectBuilder()
                .add("service-type", "point-to-point-connectivity")
                .add("service-level", "best-effort"))
        .add("nrp-cg-eth-conn-serv-spec",  Json.createObjectBuilder()
                .add("connection-type", "point-to-point")
        .add("max-frame-size", "2000")
        .add("unicast-frame-delivery", "unconditionally")
        .add("broadcast-frame-delivery", "unconditionally")
        .add("multicast-frame-delivery", "unconditionally")
        .add("ce-vlan-id-preservation", "preserve")
        .add("ce-vlan-pcp-preservation", "true")
        .add("ce-vlan-dei-preservation", "true")
        .add("s-vlan-pcp-preservation", "true")
        .add("s-vlan-dei-preservation", "true")
                .add("available-meg-level", "0")
                .add("l2cp-address-set", "ctb"))).build();



        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic YWRtaW46YWRtaW4=");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(payload.toString(), headers);

        // Send HTTP request and process result
        ResponseEntity<String> serviceCreationResponse = restTemplate.exchange(urlString, HttpMethod.POST, entity, String.class);
        System.out.println(serviceCreationResponse.toString());

        if (serviceCreationResponse.getStatusCode() == HttpStatus.OK) {
            OrderResponse orderResponse = new OrderResponse(serviceCreationResponse.toString());

            orderResponse.add(linkTo(methodOn(OrderController.class).processOrder(orderID)).withSelfRel());
            return new ResponseEntity<OrderResponse>(orderResponse, HttpStatus.OK);
        }

        OrderResponse orderResponse = new OrderResponse(String.format(FAILURE, orderID));
        orderResponse.add(linkTo(methodOn(OrderController.class).processOrder(orderID)).withSelfRel());
            return new ResponseEntity<OrderResponse>(orderResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

