package org.tuilmenau.fuml.extensions.structuredclassifiers;

import fuml.syntax.classification.Classifier;
import fuml.syntax.classification.ClassifierList;
import fuml.syntax.classification.Operation;
import fuml.syntax.classification.OperationList;
import fuml.syntax.classification.Parameter;
import fuml.syntax.classification.ParameterDirectionKind;
import fuml.syntax.classification.ParameterList;
import fuml.syntax.commonbehavior.Behavior;
import fuml.syntax.structuredclassifiers.Class_;

public class SignatureBasedDispatchStrategy extends 
		fuml.semantics.structuredclassifiers.DispatchStrategy {
	
	public fuml.syntax.commonbehavior.Behavior getMethod(
			fuml.semantics.structuredclassifiers.Object_ object,
			fuml.syntax.classification.Operation operation) {
        
		// Find the member operation of a type of the given object_ that
        // is the same as or overrides the given operation. Then
        // return the method of that operation, if it has one, otherwise
        // return a CallEventBehavior as the effective method for the
        // matching operation.
        // [If there is more than one type with a matching operation, then
        // the first one is arbitrarily chosen.]

        Behavior method = null;
        int i = 1;
        while (method == null & i <= object.types.size())
        {
            Class_ type = object.types.getValue(i - 1);
            method = getMethod(type, operation);
            i = i + 1;
        }

        return method;
	}
	
	public fuml.syntax.commonbehavior.Behavior getMethod(
			fuml.syntax.structuredclassifiers.Class_ type,
			fuml.syntax.classification.Operation operation) {
		
        Behavior method = null;
        OperationList ownedOperations = type.ownedOperation;

        // First, check if type owns or overrides the given operation.
        int i = 1;
        while (method == null & i <= ownedOperations.size())
        {
            Operation ownedOperation = ownedOperations.getValue(i - 1);

            if(operationsMatch(ownedOperation, operation))
            {
                if (ownedOperation.method.size() == 0)
                {
                    method = super.getMethod(null, ownedOperation);
                }
                else
                {
                    method = ownedOperation.method.getValue(0);
                }
            }
        }

        // If type does not own or override the given operation directly,
        // check all of it's base classes.
        if(method == null)
        {
            ClassifierList general = type.general;

            i = 1;
            while(method == null & i <= general.size())
            {
                if(general.getValue(i-1) instanceof Class_)
                {
                    method = getMethod((Class_) general.getValue(i-1), operation);
                }
            }
        }

        return method;
	}
	
	public boolean operationsMatch(
			fuml.syntax.classification.Operation ownedOperation,
			fuml.syntax.classification.Operation baseOperation) {

        // Check if the owned operation is equal to or overrides the base operation.
        // In this context, an owned operation overrides a base operation
        // if it has the same name and signature.

        boolean matches;
        if (ownedOperation == baseOperation)
        {
            matches = true;
        }
        else
        {
            matches = baseOperation.name.equals(ownedOperation.name);
            matches = matches && (baseOperation.ownedParameter.size() == ownedOperation.ownedParameter.size());
            ParameterList ownedOperationParameters = ownedOperation.ownedParameter;
            ParameterList baseOperationParameters = baseOperation.ownedParameter;
            for (int i = 0; matches == true && i < ownedOperationParameters.size(); i++)
            {
                Parameter ownedParameter = ownedOperationParameters.getValue(i);
                Parameter baseParameter = baseOperationParameters.getValue(i);

                if(ownedParameter.direction == ParameterDirectionKind.return_)
                {
                    // NOTE: In this implementation, return types may be covariant classifiers.
                    if (ownedParameter.type != baseParameter.type)
                    {

                        matches = ownedParameter.type instanceof Classifier && 
                                baseParameter.type instanceof Classifier &&
                                isCovariant((Classifier) ownedParameter.type, (Classifier) baseParameter.type);
                    }
                    else
                    {
                        matches = true;
                    }
                }
                else
                {
                    matches = ownedParameter.type == baseParameter.type;
                }
                matches = matches && (ownedParameter.multiplicityElement.lower == baseParameter.multiplicityElement.lower);
                matches = matches && (ownedParameter.multiplicityElement.upper == baseParameter.multiplicityElement.upper);
                matches = matches && (ownedParameter.direction == baseParameter.direction);
            }
        }

        return matches;
	}
	
	public boolean isCovariant(
			fuml.syntax.classification.Classifier ownedOperationReturnType,
			fuml.syntax.classification.Classifier baseOperationReturnType) {
		
        boolean isCovariant = false;

        int i = 1;
        while (isCovariant == false && i <= ownedOperationReturnType.general.size())
        {
            Classifier baseType = ownedOperationReturnType.general.getValue(i - 1);
            isCovariant = baseOperationReturnType == baseType;

            if(!isCovariant)
            {
                isCovariant = isCovariant(baseType, baseOperationReturnType);
            }

            i++;
        }

        return isCovariant;
	}
}
