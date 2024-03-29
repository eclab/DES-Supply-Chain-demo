
set datafile separator ','

#-- this does not seem to work
set grid mxtics mytic

#-- to make sure the tic labels on the Y-axis fit into the PNG image
set lmargin 8
set grid mxtics mytics

set term png size 800,600
set out 'HospitalPool.png'
set title 'Hospital/Pharmacy Pool'
plot 'HospitalPool.csv'  using ($1):($2)  with lines title 'HP.Stock', \
'HospitalPool.csv'  using ($1):($3)  with impulses title 'HP.Ordered', \
'HospitalPool.csv'  using  ($1):($4)  with lines title 'HP.Received'

set term png size 800,600
set out 'WholesalerPool.png'
set title 'Wholesaler Pool'
plot 'WholesalerPool.csv'  using ($1):($2)  with lines title 'WP.Stock', \
'WholesalerPool.csv'  using ($1):($3)  with impulses title 'WP.Ordered', \
'WholesalerPool.csv'  using  ($1):($4)  with lines title 'WP.Received', \
'UntrustedPool.csv'  using  ($1):($2)  with steps title 'Sent to WP from Untrusted Pool'

set term png size 800,600
set out 'Distributor.png'
set title 'Distribution Center'
plot 'Distributor.csv'  using ($1):($2)  with lines title 'DC.Stock', \
'Distributor.csv'  using ($1):($3==0? 1/0: $3)  with points title 'DC.Ordered', \
'Distributor.csv'  using  ($1):($4)  with lines title 'DC.Received', \
'Distributor.csv'  using  ($1):($5)  with lines title 'DC.StillOnOrder'

set term png size 800,600
set out 'Production.png'
set title "Production chain: daily output from each step's QA"
plot 'ApiProduction.csv' using ($1):($2)   with lines title 'API', \
'DrugProduction.csv'  using ($1):($2)  with lines title 'Bulk drug', \
'Packaging.csv'  using ($1):($2)  with lines title 'Packaged drug'


set term png size 800,600
set out 'DistributorReceipts.png'
set title 'Receipts by Distribution Center' 
plot [1:*] \
'Packaging.csv'  using ($1):($2)  with lines title 'From FC', \
'CmoTrackB.csv'  using ($1):($2)  with lines title 'From CMO B', \
'CmoTrackC.csv'  using ($1):($2)  with lines title 'From CMO C', \
'CmoTrackD.csv'  using ($1):($2)  with lines title 'From CMO D', \
'Distributor.csv'  using  ($1):($4)  with lines title 'Total'

set term png size 800,600
set out 'Inventory.png'
set title 'Inventory at production steps' 
plot \
'ApiProduction.csv' using ($1):($4)   with lines title 'RM => ApiProd', \
'DrugProduction.csv'  using ($1):($4)  with lines title 'API => DrugProd', \
'Packaging.csv'  using ($1):($4)  with lines  title 'Bulk Drug => Packaging'

set term png size 800,600
set out 'ProductionPlans.png'
set title 'Outstanding production plans at production steps' 
plot \
'ApiProduction.csv' using ($1):($3)   with lines title 'ApiProd', \
'DrugProduction.csv'  using ($1):($3)  with lines title 'DrugProd', \
'Packaging.csv'  using ($1):($3)  with lines  title 'Packaging'

set term png size 800,600
set out 'DistributorAnomalies.png'
set title 'DC - Receipts and anomalies'
plot [1:*] \
'Distributor.csv'  using  ($1):($4*30)  with lines title 'DC.DailyReceived*30', \
'Distributor.csv'  using  ($1):($5)  with lines title 'DC.StillOnOrder', \
'Distributor.csv'  using  ($1):(1/$8)  w p lc 'red'  title 'Anomaly'


set term png size 800,600
set out 'ApiProduction.RM.png'
set title 'RM\@ApiProduction: inventory and receipt anomalies' 
plot \
'ApiProduction.csv' using ($1):($4)   with lines title 'Inventory', \
'ApiProduction.csv' using ($1):(1/$5)  w p lc 'red' title 'Receipt anomaly'


set term png size 800,600
set out 'Safety.png'

set title 'Consumption of safety stocks' 
plot \
'ApiProduction.safety.RawMaterial.csv' using ($1):($7)   with lines title 'RM \@ ApiProd', \
'DrugProduction.safety.Api.csv' using ($1):($7)   with lines title 'API \@ DrugProd', \
'DrugProduction.safety.Excipient.csv' using ($1):($7)   with lines title 'Excipient \@ DrugProd', \
'Packaging.safety.BulkDrug.csv' using ($1):($7)   with lines title 'Bulk drug \@ Packgn', \
'Packaging.safety.PackagingMaterial.csv' using ($1):($7)   with lines title 'PacMat \@ Packgn'


exit
