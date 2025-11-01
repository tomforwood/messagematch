import {describe, expect, test} from "vitest";
import {Scenario, ScenarioMatcher} from "./ScenarioMatch";

const scenario:Scenario = {
    "scenarioName": "entityCreate",
    "expectedStates": [
        {
            "expectedCalls": []
        },
        {
            "stateTrigger": {
                "channel": {
                   // "@type": "uri",
                    "method": "PUT",
                    "statusCode": 200,
                    "path": "/{$Int=accountId}/storedNumbers"
                },
                "requestMessage": ["$Int=myVal"],
                "responseMessage": {"id":  "$Int=myId","val":["$Int=myVal"]}
            },
            "expectedCalls": [
                {
                    "channel": {
                       // "@type": "uri",
                        "method": "GET",
                        "statusCode": 200,
                        "path": "/{$Int=accountId}/storedNumbers/{$Int=myId}"
                    },
                    "requestMessage": null,
                    "responseMessage": ["$Int=myVal"]
                }
            ]
        }
    ]
}

describe("Tests for scenarioMatcher", ()=>{
    test("Should store and retrieve an entity", ()=>{
        const scenarioMatcher:ScenarioMatcher = new ScenarioMatcher(scenario)
        const httpResponse = scenarioMatcher.findMatchHttp("/5/storedNumbers", ["7"], "PUT");
        expect(httpResponse).toBeTruthy();
        const id:number = httpResponse.body["id"];
        const getResponse = scenarioMatcher.findMatchHttp("/5/storedNumbers/" + (id), null, "GET");
        expect(getResponse).toBeTruthy();
        const storedInt:number = getResponse.body[0];
        expect(storedInt).toBe(7);
    })
})