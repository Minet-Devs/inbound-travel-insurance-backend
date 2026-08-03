<!-- we need to create two more objects the travel as the Visitor entity object with his/her basic kyc attributes, 
and another object that will hold the medical benefits assigned to the visitor as VisitorBenefits entity object. 
update the documentation for this before implementation and ask any question where not clear. -->

<!-- A visitor needs to be attached to a policy, add the attribute policy that the visitor will be attached to.
also add the attribute visitorBenefit that will be the benefits assign to the visitor, such that when visitor get by id or passport number will return the visitor's kyc and the benefits assigned. update the documentation and also add tests when implementing. ensure to ask question where not clear. -->

<!-- when a visitor is created also create visitorbenefits data based on the policyid provided  -->

get visitor by-passport api should return a unique single object. the visitor object should have the list of visitor_benefits assigned