# Seldon Cloudflow integration

## TF GRPC

TF GRPC implementation is based on this [blog post](https://medium.com/@junwan01/a-java-client-for-tensorflow-serving-grpc-api-d37b5ad747aa)
After copying proto files remove this [one](protocol/src/main/protobuf/tensorflow/core/protobuf/conv_autotuning.proto)

## Seldon deployment using Helm

Here are the steps to install Seldon on k8 cluster (based on [this](https://github.com/SeldonIO/seldon-core/blob/master/notebooks/seldon_core_setup.ipynb)). Make sure that you are using Helm3
````
kubectl create namespace seldon
kubectl config set-context $(kubectl config current-context) --namespace=seldon
kubectl create namespace seldon-system
helm install seldon-core seldon-core-operator --repo  https://github.com/SeldonIO/seldon-core/tree/master/helm-charts/seldon-core-operator  --set ambassador.enabled=true --set usageMetrics.enabled=true --namespace seldon-system
kubectl rollout status deploy/seldon-controller-manager -n seldon-system
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
helm install ambassador stable/ambassador --set crds.keep=false
kubectl rollout status deployment.apps/ambassador
````
This will install Seldon and Ambassador

## Seldon deployment using Kustomize

Install Kustomize using
````
brew install kustomize
````
Clone Seldon project
````
git clone git@github.com:SeldonIO/seldon-core.git
````
Go to /operator directory
````
cd seldon-core/operator/
````
***Note*** For kubernetes <1.15 comment the patch_object_selector [here](https://github.com/SeldonIO/seldon-core/blob/master/operator/config/webhook/kustomization.yaml)

Run these 2 commands to install Seldon:
````
make install-cert-manager
make deploy-cert
````
Install Ambassador using (We are using Helm 2 here):
````
kubectl create namespace seldon
kubectl config set-context $(kubectl config current-context) --namespace=seldon
helm repo add stable https://kubernetes-charts.storage.googleapis.com/
helm repo update
helm install --name ambassador stable/ambassador --set crds.keep=false
kubectl rollout status deployment.apps/ambassador
````
## Validate Ambassador install

To validate Ambassador install, run:
````
kubectl port-forward $(kubectl get pods -n seldon -l app.kubernetes.io/name=ambassador -o jsonpath='{.items[0].metadata.name}') -n seldon 8003:8877
````
This will connect to ambassador-admin, which you can see at:

http://localhost:8003/ambassador/v0/diag/

## Creating Seldon TF deployment

To use S3 for model, first create a secret:
````
kubectl create secret generic s3-credentials --from-literal=accessKey=<YOUR-ACCESS-KEY> --from-literal=secretKey=<YOUR-SECRET-KEY>
````
After installation is complete, use deployment yaml files for [Rest](/seldonDeployments/model_tfserving_rest.yaml)
and [GRPC](/seldonDeployments/model_tfserving_grpc.yaml).

To verify that REST deployment works correctly, run the following command:
````
curl -X POST http://localhost:8003/seldon/seldon/rest-tfserving/v1/models/recommender/:predict -H "Content-Type: application/json" -d '{"signature_name":"","inputs":{"products":[[1.0],[2.0],[3.0],[4.0]],"users":[[10.0],[10.0],[10.0],[10.0]]}}'
````
To verify GRPC deployment run this [simple test](/grpcclient/src/main/scala/com/lightbend/tf/grpc/SimpleTest.scala)


Copyright (C) 2020 Lightbend Inc. (https://www.lightbend.com).

