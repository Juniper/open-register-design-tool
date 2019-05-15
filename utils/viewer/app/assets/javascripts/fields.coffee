# fields.coffee - calls getFields with input reg id 
#                      uses returned json to generate fields table
#
$ ->
  # ----- functions

  # create abbreviated category string
  createSubCatAbrString = (cat) ->
    result = ''
    if cat & 1 then result += ' INFO' 
    if cat & 2 then result += ' MAJ' 
    if cat & 4 then result += ' FAT' 
    result
  
  # create an  array of ints representing nibbles in a value (init to 0)
  createZeroNibList = (nibs) ->
     result = [0..(nibs-1)].map (i) -> 0
     result
     
  # create an array of nulls/0s representing an invalid/uninitialized value
  createNullNibList = (offset, width) ->
     result = []
     hinib = Math.floor( (offset+width-1) / 4)
     lonib = Math.floor(offset / 4)
     # alert 'lo=' + lonib + ' hi=' + hinib + ' off=' + offset + ' width=' + width + ' sum=' + (offset+width-1)
     for idx in [lonib..hinib]
        result[idx] = null
     if lonib < 1 then return result
     for idx in [0..lonib - 1]
        result[idx] = 0
     result
     
  # return list shifted left by number of bits
  leftShiftNibList = (inList, shift) ->
     #alert 'leftShiftNibList inList.length=' + inList.length + ', shift=' + shift
     if shift <= 0 then return inList   # if no shift then return array unchanged
     # --- decompose shift into bit and nibbles
     bitshift = shift %% 4
     nibshift = Math.floor(shift/4)
     listlen = inList.length
     #alert ('leftShiftNibList init=' + nibListString(inList) + ', listlen=' + listlen + ', bitshift=' + bitshift + ', nibshift=' + nibshift)
     shiftresult = []
     # --- if a bit shift is needed then do this first
     if bitshift != 0
        shiftresult[listlen] = 0
        for idx in [listlen..1]
           shiftresult[idx] = shiftresult[idx] | Math.floor((inList[idx-1] << bitshift)/16)
           shiftresult[idx-1] = (inList[idx-1] << bitshift) %% 16  
           #alert 'leftShiftNibList bit shift idx=' + idx
     # --- otherwise use input list as-is
     else      
        for val, idx in inList
           shiftresult[idx] = val
     #alert 'leftShiftNibList postbitshift=' + nibListString(shiftresult)
     if nibshift == 0 then return shiftresult
     nibshiftresult = []
     # --- now shift by number of nibbles
     for val, idx in shiftresult
        nibshiftresult[idx + nibshift] = val
        #alert 'leftShiftNibList idx=' + (idx + nibshift) + ' val=' + val
     for idx in [0..nibshift-1]
        nibshiftresult[idx] = 0
        #alert 'leftShiftNibList idx=' + (idx) + ' zero val'
     #alert 'leftShiftNibList postnibshift=' + nibListString(nibshiftresult)
     nibshiftresult 
     
  # return list shifted right by number of bits
  rightShiftNibList = (inList, shift) ->
     #alert 'rightShiftNibList inList.length=' + inList.length + ', shift=' + shift
     if shift <= 0 then return inList   # if no shift then return array unchanged
     # --- decompose shift into bit and nibbles
     bitshift = shift %% 4
     nibshift = Math.floor(shift/4)
     listlen = inList.length
     #alert ('rightShiftNibList init=' + nibListString(inList) + ', listlen=' + listlen + ', bitshift=' + bitshift + ', nibshift=' + nibshift)
     shiftresult = []
     # --- if a bit shift is needed then do this first
     if bitshift != 0
        shiftresult[0] = inList[0] >> bitshift
        for idx in [1..listlen-1]
           shiftresult[idx-1] = shiftresult[idx-1] | ((inList[idx]<< (4-bitshift)) %% 16)  # shifted bits from higher nibble  
           shiftresult[idx] = inList[idx] >> bitshift
           #alert 'rightShiftNibList bit shift idx=' + idx
     # --- otherwise use input list as-is
     else      
        for val, idx in inList
           shiftresult[idx] = val
     #alert 'rightShiftNibList postbitshift=' + nibListString(shiftresult)
     if nibshift == 0 then return shiftresult
     nibshiftresult = []
     # --- now shift by number of nibbles
     for val, idx in shiftresult
        if idx >= nibshift
           nibshiftresult[idx - nibshift] = val
           #alert 'rightShiftNibList idx=' + (idx - nibshift) + ' val=' + val
     #alert 'rightShiftNibList postnibshift=' + nibListString(nibshiftresult)
     nibshiftresult 
      
  # create an  array of ints from a hex input string, offset index and width
  createNibList = (hexString, offset, width) ->
     result = []
     # insure hexString is valid
     maxnibs = Math.ceil(width/4)
     #alert maxnibs + "--" + hexString.length
     if !hexString? or (hexString.length == 0) or (hexString.length > maxnibs) then return createNullNibList(offset, width)
     # --- convert string chars to ints
     residx = 0
     for stridx in [hexString.length..1]
        result[residx] = parseInt( hexString.substring(stridx - 1, stridx), 16)
        if isNaN(result[residx]) then return createNullNibList(offset, width)
        #alert 'createNibList string parse residx=' + residx
        residx++
     # --- check width by validating number of bits in hi array elem
     hiNibBits = width %% 4
     if hiNibBits > 0 and residx == maxnibs and result[residx-1] > Math.pow(2, hiNibBits)-1 then return createNullNibList(offset, width)
     # --- if a short input string then pad with zeroes
     if residx < maxnibs
        for idx in [residx..maxnibs-1]
           result[idx] = 0
     #alert 'createNibList preshift=' + nibListString(result)
     result = leftShiftNibList(result, offset)
     #alert 'createNibList postshift=' + nibListString(result)
     result

  # update a nib array of ints with another newly created list
  updateNibList = (oldlist, hexString, offset, width) ->
     #alert 'updateNibList oldlist=' + nibListString(oldlist)
     result = []
     modlist = createNibList(hexString, offset, width)
     #alert 'updateNibList modlist=' + nibListString(modlist)
     for val, idx in oldlist
       if !val?
         result[idx] = null
       else if (idx < modlist.length)
         result[idx] = if modlist[idx]? then (val | modlist[idx]) else null
       else 
         result[idx] = val
       #alert 'idx=' + idx + ' val=' + result[idx]
     #alert 'updateNibList result=' + nibListString(result)
     result

  # extract a sub array of nibbles at offset/width from another list
  extractNibList = (baselist, offset, width) ->
     #alert('extractNibList baselist=' + nibListString(baselist) + ', offset=' + offset + ', width=' + width)
     # --- first shift right by offset
     shiftlist = rightShiftNibList(baselist, offset)
     #alert('extractNibList shiftlist=' + nibListString(shiftlist))
     # --- now truncate the list by width
     result = []
     nibbles = Math.ceil(width/4)
     for val, idx in shiftlist
        if idx < nibbles
           result[idx] = val
           #alert 'extractNibList idx=' + idx + ' val=' + val
     # --- clean up the high index
     validbits = width %% 4
     hidx = result.length - 1
     if validbits > 0
        result[hidx] = result[hidx] & (Math.pow(2, validbits)-1)
     #alert 'extractNibList result=' + nibListString(result)
     result

  # create an output value string from a nibble value array
  nibListString = (nibArray) ->
     if nibArray? and nibArray.length
        # --- find index of highest non-zero nibble
        maxidx = nibArray.length - 1
        while maxidx > 0 and nibArray[maxidx]? and nibArray[maxidx] == 0
           maxidx--
        # while  TODO 
        result = ''
        for idx in [0..maxidx]
           nib = nibArray[idx]
           ntxt = if nib? then nib.toString(16) else '?'
           divider = if (idx > 0) and (idx %% 8 == 0) then '_' else ''
           result = ntxt + divider + result
           #alert 'nibListString: idx=' + idx + ', divider=' + divider + ', ntxt=' + ntxt + ', res=' + result
        result
     else   
        ''

  # ---- update reg value using field resets or specified values
  updateRegValue = (isReset) ->
     # --- get reg width
     regwidth = $("#regwidth").data('width')
     # ----- adjust size of the register value
     regValSize = Math.min(100, Math.floor( regwidth/4 * 1.125 ))
     regValueElem = $("#regval")
     $("#regval").attr("size", regValSize);
     #regValueElem.css("width", (maxnibs + 4)*8);
     # --- initialize calculated value using reg width
     compval = createZeroNibList(regwidth/4)
     # ---- for each field value input, calc new result
     $(".valinput").each (validx, val) ->
        #alert('index=' + validx)
        width = parseInt(val.getAttribute('data-width'), 10)
        lowIdx = parseInt(val.getAttribute('data-lowidx'), 10)
        if isReset
           fldVal = val.getAttribute('data-reset')
           val.value = fldVal
        else
           fldVal = val.value
        compval = updateNibList(compval, fldVal, lowIdx, width)
     # ---- change the calculated val text
     $("#regval").val(nibListString(compval))

  # ---- clear all field values
  clearFieldValues = ->
     $(".valinput").each (validx, val) ->
        val.value = '0'

  # ---- set all field values from a reg value
  setFieldValuesFromReg = ->
     # --- get the reg value
     regwidth = $("#regwidth").data('width')
     regstr = $("#regval").val() 
     regstr = regstr.replace(/_/g, '')
     valueOK = checkRegValue()
     regval = createNibList(regstr, 0, regwidth)  # extract reg val into nibble list
     #alert ('setFieldValuesFromReg str=' + regstr + ', val=' + nibListString(regval) + ', regwidth=' + regwidth)
     $(".valinput").each (validx, val) ->
        #alert('index=' + validx)
        width = parseInt(val.getAttribute('data-width'), 10)
        offset = parseInt(val.getAttribute('data-lowidx'), 10)
        if !valueOK
           val.value = '?'
        else
           newval = extractNibList(regval, offset, width)
           #alert ('setFieldValuesFromReg regval=' + nibListString(regval) + ', fval=' + nibListString(newval) + ', offset=' + offset + ', width=' + width)  #  
           val.value = nibListString(newval)

  # ---- check register input value / return false and set val=? if invalid
  checkRegValue = ->
     regwidth = $("#regwidth").data('width')
     maxnibs = regwidth/4
     inputstr = $("#regval").val()
     inputstr = inputstr.replace(/_/g, '')
     # ---- verify valid length
     if inputstr.length > maxnibs
        #alert 'checkRegValue input len=' + inputstr.length + ', max=' + maxnibs
        $("#regval").val('?')
        return false
     # ---- verify valid pattern
     hexpattern = /// ^        # begin of line
                  ([a-f\d]+)  # at least one nibble
                  $ ///i       # eol, ignore case
     if !inputstr.match hexpattern
        $("#regval").val('?')
        return false
     true

  # ----- display the field table
  fldIdElems = $("#fields")
  regId = fldIdElems.first().data('id')
  #  alert("regId=" + regId)
  rr = jsRoutes.controllers.Application.getFields(regId)
  #alert("type=" + rr.type + ", url=" + rr.url)
  $.get rr.url, (fields) ->
    # ---- attach on-change function to register value
    $("#regval").change ->
       setFieldValuesFromReg()
    # ---- attach on-click function to reset button
    $("#resetbtn").click ->
       updateRegValue(true)
    # ---- attach on-click function to clear button
    $("#clearbtn").click ->
       clearFieldValues()
       updateRegValue(false)
    # -- build field table header
    hrow = $("<tr>")
    hrow.addClass("active")
    hrow.append $("<th>name</th>")
    hrow.append $("<th>width</th>")
    hrow.append $("<th>bit(s)</th>")
    hrow.append $("<th>reset (0x)</th>")
    hrow.append $("<th>value (0x)</th>")
    hrow.append $("<th>access</th>")
    hrow.append $("<th>vol</th>")
    hrow.append $("<th>cntr</th>")
    hrow.append $("<th>intr</th>")
    hrow.append $("<th>dc</th>")
    hrow.append $("<th>sub</th>")
    hrow.append $("<th>short text</th>")
    hrow.append $("<th></th>")
    hdr = $("<thead>").append hrow
    fldIdElems.append hdr   
    bod = $("<tbody>")
    fldIdElems.append bod   
    # --- build a row per field
    $.each fields, (index, field) ->
      brow = $("<tr>")
      txt = field.name
      brow.append $("<td>#{txt}</td>")
      brow.append $("<td>#{field.width}</td>")
      txt = if field.width > 1 then field.lowIdx + ' - ' + (field.lowIdx+field.width-1) else '' + field.lowIdx
      brow.append $("<td>#{txt}</td>")
      #brow.append $("<td>#{field.lowIdx}</td>")
      #hibit = field.lowIdx+field.width-1
      #brow.append $("<td>#{hibit}</td>")
      rsttxt = field.rst
      rsttxt ?= ""
      brow.append $("<td>#{rsttxt}</td>")
      # --- create input for value calc (only 8 chars max allowed for now)
      valinputEl = $('<input class="valinput" data-width="' + field.width + '" data-lowidx="' + field.lowIdx + '" data-reset="' + rsttxt + '"type="text" maxlength="' + Math.min(8, Math.ceil(field.width/4)) + '" value="0">')
      tdEl = $('<td>').append valinputEl
      brow.append tdEl 
      # ---- attach on-change function to input elements
      valinputEl.change ->
         updateRegValue(false)
      brow.append $("<td>#{field.access}</td>")
      txt = if field.hwMod then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      txt = if field.counter then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      txt = if field.intr then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      txt = if field.dontcompare then 'y' else ''
      brow.append $("<td>#{txt}</td>") 
      brow.append $("<td>#{createSubCatAbrString(field.subCatCode)}</td>")
      txt = field.shortText ? ""
      brow.append $("<td>#{txt}</td>")
      txt = '<button type="button" class="btn btn-info btn-sm" data-toggle="collapse" data-target="#collapse' + field.id + '">...</button>'
      brow.append $("<td>#{txt}</td>")
      bod.append brow    
      # --- now build a collapsable row for addl info
      txt = '<tr class="collapse out" id="collapse' + field.id + '">'
      brow = $(txt)
      brow.append $("<td></td>") 
      brow.append $("<td></td>") 
      txt = field.longText ? ""
      txt = '<td colspan="10" class="info"><b>' + field.name + '</b><br>' + txt + '</td>'
      brow.append $(txt)
      brow.append $("<td></td>") 
      bod.append brow
      
