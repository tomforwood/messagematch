import {BASE_URL} from "./constants.ts";

export type ClientResponse = {
    clients: ClientDetails[];
}

export type ClientDetails = {
    name: string,
    versions: string[],
}

type Versions = {
    [version: string] : boolean
}

type ApiTestVersion = {
    [testScenario: string]: Versions
}

type ApiCompatibilities = {
    name: string;
    allVersions: APIVersion[];
    scenariosTestedByVersions: ApiTestVersion;
}

type APIVersion = {
    versionTag: string;
    lastTestTime: string;
}

export type CompatMatrix = {
    apis : ApiCompatibilities[],
    untestedScenarios: string[]
}

export async function getClients(): Promise<ClientResponse> {
    return await fetch(BASE_URL + '/clients')
        .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error(`Backend service returned: ${response.statusText} (${response.status})`);
                }
            }
        );
}


export async function getMatrix(client:ClientDetails, version:string): Promise<CompatMatrix> {
    return await fetch(BASE_URL + '/matrix' + "?client="+client.name+
        '&clientVersion='+version)
        .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error(`Backend service returned: ${response.statusText} (${response.status})`);
                }
            }
        );
}