import {PathMatcher} from "../match/PathMatcher";
import {JsonMatcher} from "../match/JsonMatcher";
import JsonGenerator from "../generate/JsonGenerator";

class Channel {
    constructor(readonly path: string, readonly method: String, readonly statusCode: number, readonly statusLine?:string) {}
}

class CallExample {
    constructor(public readonly channel:Channel, public readonly name?: string, public requestMessage?: any, public responseMessage?:any) { }
}

class State {
    constructor(public readonly expectedCalls:CallExample[], public readonly stateTrigger?: CallExample) {}
}

export class Scenario {
    constructor(public readonly scenarioName: string, public readonly expectedStates:State[] ) {}
}

class HttpResponse {
    constructor(public readonly statusCode:number, public readonly body:any) {
    }
}

export class ScenarioMatcher {

    bindings:Record<string, any> = {};
    state:number=0;

    constructor(private readonly scenario:Scenario ) {}

    public findMatchHttp(path:string, body:any, method:string):HttpResponse|null{
        const currentState = this.scenario.expectedStates[this.state];
        let matched = false;
        for (const call of currentState.expectedCalls) {
            const updatedBinding = {...this.bindings};
            matched = this.matches(call, path, method, body, updatedBinding);
            if (matched) {
                console.log("matched")
                return this.sucessfullMatch(call, updatedBinding)
            }
        }
        if (this.state < this.scenario.expectedStates.length-1) {
            const newStateNum= this.state + 1;
            const newState = this.scenario.expectedStates[newStateNum];
            const stateTrigger = newState.stateTrigger;
            if (!stateTrigger) {
                throw new Error("state trigger cannot be null on subsequent states")
            }
            const updatedBinding = {...this.bindings};

            matched = this.matches(stateTrigger, path, method, body, updatedBinding);
            if (matched) {
                this.state = newStateNum;
                console.log("matched")
                return this.sucessfullMatch(stateTrigger, updatedBinding)
            }

        }
        return null;
    }

    private sucessfullMatch(call:CallExample, updatedBindings:Record<string, any>):HttpResponse {
        const statusCode = call.channel.statusCode || 200;
        if (call.responseMessage) {
            const generator = new JsonGenerator(call.responseMessage, updatedBindings);
            const result = generator.generate();
            this.bindings = {...updatedBindings, ...generator.getBindings()};
            return new HttpResponse(statusCode, result);
        }
        else
        {
            this.bindings = updatedBindings;
            return new HttpResponse(statusCode, null);
        }
    }

    private matches(call: CallExample, path: string, method: string, body: string, updatedBinding: Record<string, any>): boolean {
       if (call.channel.method !== method)
           return false;
       //The JS path matcher (unlike the java one) does not  copy the bindings
       const pathMatcher = new PathMatcher(call.channel.path, path, updatedBinding);
       if (!pathMatcher.matches()) return false;
       if (!call.requestMessage) {
           return true;
       } else {
           const bodyMatcher:JsonMatcher = new JsonMatcher(call.requestMessage, body);
           if (bodyMatcher.matches()) {
               //TODO mutate in place
               Object.entries(bodyMatcher.getBindings()).forEach(binding => {
                   updatedBinding[binding[0]]=binding[1];
               })
               return true;
           }
       }
       return false;
    }
}