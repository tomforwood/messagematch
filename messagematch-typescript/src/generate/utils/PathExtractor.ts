export class PathExtractor {
    static extractPrimitiveNode(path: string, bindings: Record<string, any>): any {
        // Stub implementation - to be completed later
        return bindings[path.substring(4)]; // removes "$ID." prefix
    }
}