# home-flux-kube

## Setup your FluxCD with this 

```bash
# Create a GitHub personal access token and export it as an env var
export GITHUB_TOKEN=<my-token>

# Run bootstrap for a public repository on a personal account
flux bootstrap github --owner=coljurten --repository=home-flux-kube --private=false --personal=true --path=clusters/my-cluster
```
